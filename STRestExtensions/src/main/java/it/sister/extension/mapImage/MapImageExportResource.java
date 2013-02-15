package it.sister.extension.mapImage;

import it.sister.extension.ApplicationResources;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.gson.Gson;

public class MapImageExportResource extends AbstractCatalogResource {	

		public MapImageExportResource(Context context, Request request,
				Response response, Catalog catalog) {
			super(context, request, response, String.class, catalog);
		}

		@Override
		protected List<DataFormat> createSupportedFormats(Request request,
				Response response) {
			List<DataFormat> formats = new ArrayList<DataFormat>();
			formats.add(new StringFormat(MediaType.APPLICATION_JSON));
			formats.add(new StringFormat(MediaType.TEXT_XML));			
			return formats;
		}

				
		@Override
		protected String handleObjectGet() throws Exception {
			String imgMapUrl = mapImageExportPath();	
			Gson gson = new Gson();
			PathResponse res = new PathResponse(imgMapUrl);
			String imgMapUrlJson = gson.toJson(res);			
			return imgMapUrlJson;
		}		

		@Override
		public boolean allowPost(){
			return true;
		}
		
		@Override
		protected String handleObjectPost(Object object) throws Exception {
			String imgMapUrl = mapImageExport(object);
			return imgMapUrl;
		}
		
		 /**
		 * Restituisce l'url completo dell'immagine della mappa passata come parametro (tra i parametri c'è fileName il nome dell'immagine della mappa)
		 * @return	l'url risolto
		 * @throws IOException
		 */		
		private String mapImageExportPath() throws Exception {
			 //Get request parameters		
			String wsName = getAttribute("fileName");
			String mapImageUrl = ApplicationResources.getApplicationProperty("mapImageUrl", "");
			String mapImagePath = ApplicationResources.getApplicationProperty("mapImagePath", "");
			String imgPath = mapImagePath;			
			if (!mapImagePath.endsWith("/") && !mapImagePath.endsWith("\\"))
			{
				imgPath += "/";				
			}
			imgPath+=wsName;

			File f = new File(imgPath);			
			if(f.exists()) {
				imgPath = imgPath.replaceAll(mapImagePath, mapImageUrl);
				return imgPath; 
				}
			else{
				return "";
			}
		}
		
		 /**
		 * Crea l'immagine di mappa sovrapponendo i tiles passati come parametro
		 * L'oggetto passago come parametro è un oggetto json di tipo ExportImageInfo.
		 * @throws IOException
		 */	
		private String mapImageExport(Object object) throws Exception {
			if (object==null){
				return "";
			}
			String jsonStr = object.toString();
			Gson gson = new Gson();
			ExportImageInfo exportImgInfo = gson.fromJson(jsonStr, ExportImageInfo.class);
			String mapImagePath = ApplicationResources.getApplicationProperty("mapImagePath", "");			
			
			String imagePath = createImageFromLayers(exportImgInfo.getLayers(), mapImagePath, exportImgInfo.getFileName(), exportImgInfo.getWidth(), exportImgInfo.getHeight()); 			
			return imagePath;
		}
				
				
		/**
		 * Crea un'immagine sovrapponendo un insieme di layers passati come parametro
		 * @param layers	insieme di layer
		 * @param imageFolder	cartella di destinazione per le immagini (con diritti di lettura/scrittura)
		 * @param width	larghezza dell'immagine risultante
		 * @param height	altezza dell'immagine risultante
		 * @return	il path dell'immagine creata
		 * @throws IOException
		 */
		private static String createImageFromLayers(ImageLayer[] layers, String imageFolder, String mapImageName, int width, int height) throws IOException{
			// si scorrono tutti i layer passati. 
			for(ImageLayer l : layers){
				// se non c'è il path dell'immagine si scarica
				if(l.getImagePath() == null || l.getImagePath().equalsIgnoreCase("")){
					String randomFileName = new BigInteger(130, new SecureRandom()).toString(32) + ".png";
					String filePath = imageFolder + (imageFolder.endsWith("/") ? "" : "/") + randomFileName;
					
					if(l.getRequestType() == RequestType.GET){
						String urlComposed = l.getCompleteUrl() /*+ ((l.getParameters() != null && !l.getParameters().equalsIgnoreCase("")) ? "?" + l.getParameters() : "")*/;
						downloadFileViaGET(urlComposed, filePath);
					}else if(l.getRequestType() == RequestType.POST){
						downloadFileViaPOST(l.getBasicUrl(), l.getParameters(), filePath);
					}
					// si imposta il path dell'immagine scaricata
					l.setPath(filePath);
				}
			}
			
			String fileResultName = new BigInteger(130, new SecureRandom()).toString(32) + ".png";
			fileResultName = mapImageName;
			String fileResultPath = imageFolder + (imageFolder.endsWith("/") ? "" : "/") + fileResultName;
			saveImageOverlapped(layers, width, height, fileResultPath);
			
			return fileResultPath;
		}
		
		
		/**
		 * Sovrappone le immagini passate (con il relativo livello di trasparenza) e le salva in png
		 * @param layers	immagini da sovrapporre
		 * @param width	larghezza dell'immagine risultante dalla sovrapposizione
		 * @param height	altezza dell'immagine risultante dalla sovrapposizione
		 * @param destinationFilePath	path di destinazione del file risultato dell'operazione
		 * @throws IOException
		 */
		private static void saveImageOverlapped(ImageLayer[] layers, int width, int height, String destinationFilePath) throws IOException{
			// si compone l'array di immagini applicandoci la trasparenza
			BufferedImage[] images = new BufferedImage[layers.length];
			for(int i = 0; i< layers.length; i++){
				BufferedImage image = ImageIO.read(new File(layers[i].getImagePath()));
				setApha(image, layers[i].getAlpha());
				images[i] = image;
			}
			
			// si crea l'immagine, sovrapponendo le immagini create 
			BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			Graphics g = combined.getGraphics();
			
			for(BufferedImage bi : images){
				g.drawImage(bi, 0, 0, null);
			}

			// si salva l'immagine come png
			ImageIO.write(combined, "PNG", new File(destinationFilePath));
		}
		
		/**
		 * Imposta la trasparenza desiderata all'immagine passata tra i parametri
		 * @param image immagine sulla quale applicare la trasparenza
		 * @param alpha	valore della trasparenza (da 0 a 255)
		 */
		private static void setApha(BufferedImage image, int alpha){
			byte alphaB = (byte)alpha;
			alphaB %= 0xff; 
		    for (int cx=0;cx<image.getWidth();cx++) {          
		        for (int cy=0;cy<image.getHeight();cy++) {
		            int color = image.getRGB(cx, cy);

		            int mc = (alphaB << 24) | 0x00ffffff;
		            int newcolor = color & mc;
		            image.setRGB(cx, cy, newcolor);
		        }
		    }
		}
		
		/**
		 * Scarica un file a partire dall'URL (con relativi parametri) via POST
		 * @param request url del servizio
		 * @param urlParameters	parametri dell'url
		 * @param destinationFilePath path di destinazione del file
		 * @throws IOException
		 */
		private static void downloadFileViaPOST(String request, String urlParameters, String destinationFilePath) throws IOException{
			URL url = new URL(request); 
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();           
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setInstanceFollowRedirects(false); 
			conn.setRequestMethod("POST"); 
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			conn.setUseCaches (false);

			DataOutputStream wr = new DataOutputStream(conn.getOutputStream ());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			conn.disconnect();

			InputStream is = conn.getInputStream();
		    
		    OutputStream out=new FileOutputStream(new File(destinationFilePath));
		    byte buf[]=new byte[1024];
		    int len;
		    while((len=is.read(buf))>0){
		    	out.write(buf,0,len);
		    }
		    
		    wr.close();
		    out.close();
		    is.close();
		}
		
		/**
		 * Scarica un file a partire dall'URL via GET
		 * @param request	url della richiesta
		 * @param destinationFilePath	path di destinazione del file
		 * @throws IOException
		 */
		private static void downloadFileViaGET(String request, String destinationFilePath) throws IOException{
			URL website = new URL(request);
		    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		    FileOutputStream fos = new FileOutputStream(new File(destinationFilePath));
		    fos.getChannel().transferFrom(rbc, 0, 1 << 24);
		}
}
