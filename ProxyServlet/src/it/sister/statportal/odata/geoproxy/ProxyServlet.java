package it.sister.statportal.odata.geoproxy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Proxy che si posiziona tra il client javascript e il geoserver
 * e permette di aggiungere alla richiesta l'header di autenticazione
 * evitando così che nome utente e password viaggino in chiaro 
 * in una richiesta javascript.
 */
public class ProxyServlet extends HttpServlet {
	
	protected static Logger logger = Logger.getLogger(ProxyServlet.class);	
	
	private static final long serialVersionUID = 1L;
      
	/**
	 * Indirizzo del server a cui redirigere le richieste
	 */
	private static String mapTo = null;
	
	/**
	 * Nome utente da aggiungere all'header di autenticazione
	 */
	private static String serverUsername = null;
	
	/**
	 * Password da aggiungere all'header di autenticazione
	 */
	private static String serverPassword = null;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ProxyServlet() {
        super();
    }

   @Override
   public void init(ServletConfig config) throws ServletException {
		super.init(config);
		//indirizzo a cui devo redirigere le chiamate
		mapTo = config.getInitParameter("mapTo");
		if(mapTo == null || mapTo.equals("")){
			throw new ServletException("Il parametro mapTo è obbligatorio, modificare il file web.xml");
		}
		//nome utente e password per l'accesso al server su cui si redirigono le chiamate
		//possiamo gestire il caso di nome utente e password nulli nel caso l'autenticazione non sia necessaria
		serverUsername = config.getInitParameter("serverUsername");
		serverPassword = config.getInitParameter("serverPassword");
		try{
			String logConfigFile = config.getInitParameter("logConfigFile");
			DOMConfigurator.configureAndWatch(logConfigFile);
		}catch(Exception ex){
			//si perde il logging ma l'applicazione continua a funzionare
		}
   }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {		
		logger.info("Arrivo chiamata in GET: " + request.getRequestURL()+(request.getQueryString() != null ? "?"+request.getQueryString() : ""));
		//inoltro la richiesta al server, prendo il risultato e lo giro al client
		String redirectUrl = getRedirectUrl(request);
		try{
			String result = redirectGet(request, redirectUrl);
			sendResponse(request, response, result);
		}catch(Exception ex){
			logger.info("Invio errore 500 per eccezione: " + ex.getMessage());
			response.sendError(500, ex.getMessage());
		}
	}	
	
	/**
	 * Redirige una richiesta in GET ad una url
	 * @param request la richiesta da redirigere
	 * @param redirectUrl l'url a cui viene rediretta
	 * @return ciò che viene letto dalla risposta
	 * @throws Exception 
	 */
	protected String redirectGet(final HttpServletRequest request, final String redirectUrl) throws Exception{
		logger.info("Redirezione chiamata a: "+redirectUrl);
		String retVal = "";
		URL u;
		HttpURLConnection uc = null;
		InputStream content = null;
		ByteArrayInputStream bais = null;
		GZIPInputStream gzis = null;
		InputStreamReader reader = null;
		BufferedReader boh = null;
		try {
			u = new URL(redirectUrl);
			uc = (HttpURLConnection)u.openConnection();
			addHeaders(request, uc);
			content = (InputStream) uc.getInputStream();
			byte[] bytes = new byte[uc.getContentLength()];
			org.apache.axis.utils.IOUtils.readFully(content, bytes);
			bais = new ByteArrayInputStream(bytes);	
			gzis = new GZIPInputStream(bais);
			reader = new InputStreamReader(gzis);
			boh = new BufferedReader(reader);			
			String tmp = "";
			while ((tmp = boh.readLine()) != null){
				retVal+= tmp;
			}
				
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		} finally{
			if(content != null){
				try {
					content.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}				
			}
			if(bais != null){
				try {
					bais.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}				
			}
			if(gzis != null){
				try {
					gzis.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}				
			}
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}				
			}
			if(boh != null){
				try {
					boh.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					throw e;
				}				
			}
		}
		return retVal;
	}
	
	/**
	 * Redirige una richiesta in POST ad una url predefinita
	 * @param request la richiesta da redirigere
	 * @param redirectUrl la url a cui redirigerla
	 * @return il codice risultante
	 */
	protected int redirectPost(final HttpServletRequest request, final String redirectUrl){
		URL u;
		HttpURLConnection httpUrlConnection;
		OutputStream outputStream = null;
		DataOutputStream wr = null;
		StringBuffer jb = new StringBuffer();
		String line = null;
		int status;
		BufferedReader reader = null;

		try {
			u = new URL(redirectUrl);
			httpUrlConnection = (HttpURLConnection)u.openConnection();
			httpUrlConnection.setDoOutput(true);
			addHeaders(request, httpUrlConnection);
			outputStream = httpUrlConnection.getOutputStream();
			reader = request.getReader();
			while ((line = reader.readLine()) != null){
				jb.append(line);
			}
			wr = new DataOutputStream(outputStream);
			wr.writeBytes(jb.toString());
			wr.flush();
			try {
				wr.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return 500;
			}
			status = httpUrlConnection.getResponseCode();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return 500;
		} finally{
			if(outputStream != null){
				try {
					outputStream.close();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}								
			}
			if(reader != null){
				try{
					reader.close();
				}catch(IOException ioe){
					logger.warn(ioe.getMessage(), ioe);
				}
			}
		}		
		return status;							
	}

	/**
	 * Aggiunge ad una connessione ad un url http tutti gli header contenuti nella richiesta
	 * più un ulteriore header di autenticazione nel caso sia stato specificato un nome utente e una password
	 * @param request la richiesta da cui prendere gli header
	 * @param httpUrlConnection la connessione su cui aggiungere gli header
	 * @throws ProtocolException
	 */
	private void addHeaders(final HttpServletRequest request, HttpURLConnection httpUrlConnection) throws ProtocolException {
		//scorro tutti gli header della richiesta e li aggiungo
		Enumeration<String> headerNames = request.getHeaderNames();
		while(headerNames.hasMoreElements()){
			String headerName = headerNames.nextElement();
			httpUrlConnection.setRequestProperty(headerName, request.getHeader(headerName));
		}
		httpUrlConnection.setRequestMethod(request.getMethod());
		//se il nome utente è presente nella configurazione imposto anche l'header Authorization
		if (serverUsername != null) {
			httpUrlConnection.setRequestProperty("Authorization", "Basic "+ encode(serverUsername+ ":" + serverPassword));
		}
	}

	/**
	 * Codifica il parametro usando un BASE64Encoder
	 * @param source la stringa da codificare
	 * @return il parametro codificato
	 */
	private static String encode(String source) {
		sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
		return (enc.encode(source.getBytes()));
	}
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		logger.info("Arrivo chiamata in POST: " + request.getRequestURL());
		//inoltro la richiesta al server, prendo il risultato e lo giro al client
		String redirectUrl = getRedirectUrl(request);		
		int responseCode = redirectPost(request, redirectUrl);
		if(responseCode < 200 || responseCode >= 300){
			logger.info("Invio errore "+ responseCode);
			response.sendError(responseCode);
		}else{
			sendResponse(request, response, "");
		}
	}

	/**
	 * Calcola l'indirizzo a cui redirigere la richiesta.
	 * Le informazioni vengono estratte dalla richiesta originale e 
	 * sostituite con quelle impostate nel file di configurazione web.xml
	 * @param request la richiesta http
	 * @return l'indirizzo a cui redirigere la richiesta
	 */
	private String getRedirectUrl(final HttpServletRequest request){
		//prendo la richiesta e ne estraggo l'indirizzo richiesto
		String requestedUrl = request.getRequestURL().toString();
		//inoltro la richiesta al server
		int index = requestedUrl.lastIndexOf(this.getServletContext().getContextPath());
		//String redirectUrl = requestedUrl.replaceAll(request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+this.getServletContext().getContextPath(), mapTo);
		String redirectUrl = mapTo + requestedUrl.substring(index+this.getServletContext().getContextPath().length());
		//in caso ci siano dei parametri in nella query string li aggiungo
		final String queryString = request.getQueryString();
		if(queryString != null && !queryString.equals("")){
			redirectUrl += "?" + queryString;
		}
		return redirectUrl;
	}
	
	/**
	 * Invia una risposta contenente il valore indicato
	 * @param request la richiesta
	 * @param response la risposta
	 * @param result il contenuto della risposta
	 * @throws IOExceptions
	 */
	private void sendResponse(final HttpServletRequest request, final HttpServletResponse response, final String result) throws IOException{
		byte[] resultBytes = result.getBytes();
		response.setContentLength(resultBytes.length);
		response.setContentType(request.getContentType());
		PrintWriter printWriter = null;
		try{
			printWriter = response.getWriter();
			printWriter.print(result);
			printWriter.flush();
		} catch(IOException ioe){
			logger.error(ioe.getMessage(), ioe);
			throw ioe;
		}
	}
}
