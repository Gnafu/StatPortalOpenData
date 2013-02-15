package it.sister.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Properties;


/**
 * 
 */
public class DBManager {
	
	/* Fields */
	private Connection conn;

	/**
	 * 
	 */
	public DBManager() {	
	}
	
	/**
	 * Crea una connessione al database PostgreSql
	 * @param host, host del database
	 * @param port, porta del database
	 * @param databaseName, nome del database
	 * @param user, nome utente per accedere al database
	 * @param passw, password per accedere al database
	 * @return true se la connessione è stata creata, altrimenti restituisce false
	 */
	public boolean createPostgreSQLConnection(String host,String port,String databaseName,String user,String passw) throws Exception{		
		String url = "jdbc:postgresql://" + host + ":" +port + "/" + databaseName;
		Properties props = new Properties();
	    props.setProperty("user",user);
	    props.setProperty("password",passw);
		conn = DriverManager.getConnection(url, props);
		return true;
	}
	
	/**
	 * Crea una vista
	 * @param viewName, nome da assegnare alla vista
	 * @param viewDefinition, definizione della vista
	 * @return true se la vista è stata creata, altrimenti restituisce false
	 */
	public boolean createView(String viewName,String viewDefinition){
		if(conn!=null){
			try{
				if(!conn.isClosed()){					
					Statement stmt = conn.createStatement();
					String query = "CREATE VIEW \"" + viewName + "\" AS " + viewDefinition;
					stmt.execute(query);
					return true;
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return false;
				}
			}catch(SQLException sqle){
				STUtils.writeErrorMessage(sqle.getMessage());
				return false;
			}
		}else{
			return false;
		}
	}
	
	
	/**
	 * Nomi dei campi presenti nella tabella.
	 * Vengono esclusi i campi di sistema: tableoid, cmax, xmax, cmin, xmin, ctid 
	 * @param tableName, nome della tabella
	 * @return lista dei campi presenti nella tabella
	 */
	public LinkedList<String> getTableFields(String tableName){
		if(conn!=null){			
			try{
				if(!conn.isClosed()){
					LinkedList<String> fields = new LinkedList<String>();
					Statement stmt = conn.createStatement();
					String query = "SELECT attname FROM pg_attribute , pg_type WHERE typrelid=attrelid AND typname = '" + tableName + "'" ;
					ResultSet rs = stmt.executeQuery(query);				
					while (rs.next()) {
						String attName = rs.getString("attname");					
						if(!(attName.equals("tableoid")||attName.equals("cmax")||attName.equals("xmax")||attName.equals("cmin")||attName.equals("xmin")||attName.equals("ctid"))){
							fields.add(attName);
						}
					}
					return fields;
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return null;
				}
			}catch(SQLException sqle){
				STUtils.writeErrorMessage(sqle.getMessage());
				return null;
			}
		}else{
			return null;
		}
	}
	
	public boolean executeQuery(String query){
		if(conn!=null){
			try{
				if(!conn.isClosed()){					
					Statement stmt = conn.createStatement();					
					stmt.execute(query);
					return true;
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return false;
				}
			}catch(SQLException sqle){
				STUtils.writeDebugMessage("Errore durante esecuzione della query: " + query);
				return false;
			}
		}else{
			return false;
		}
	}
	
	public int getNumRecords(String tableName){
		if(conn!=null){
			String query="";
			int numRecords=0;
			try{
				Statement stmt = conn.createStatement();
				query = "SELECT COUNT(*) AS num_rec FROM \"" + tableName +"\"";
				ResultSet rs = stmt.executeQuery(query);				
				while (rs.next()) {
					numRecords = rs.getInt("num_rec");
					break;
				}
				return numRecords;
			}catch(SQLException sqle){
				STUtils.writeDebugMessage("Errore durante esecuzione della query: " + query);
				return -1;
			}
		}else{
			return -1;
		}
	}
	
	/**
	 * Check if table exists
	 * @param tableName: name of the table
	 * @return true if table exists, false otherwise
	 */
	public boolean tableExists(String tableName){
		if(conn!=null){	
			ResultSet rs=null;		
			try{
				if(!conn.isClosed()){					
					Statement stmt = conn.createStatement();
					String query = "select * from pg_tables where tablename='" + tableName + "'" ;
					rs = stmt.executeQuery(query);
					if(rs.next()){
						rs.close();
						return true;
					}else{
						rs.close();
						return false;
					}			
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return false;
				}
			}catch(SQLException sqle){
				STUtils.writeErrorMessage(sqle.getMessage());
				return false;
			}
		}else{
			STUtils.writeDebugMessage("Connessione al database chiusa");
			return false;
		}
	}
	
	/**
	 * Check if view exists
	 * @param tableName: name of the view
	 * @return true if view exists, false otherwise
	 */
	public boolean viewExists(String viewName){
		if(conn!=null){	
			ResultSet rs=null;		
			try{
				if(!conn.isClosed()){					
					Statement stmt = conn.createStatement();					
					String query = "select * from pg_views where viewname='" + viewName + "'" ;
					rs = stmt.executeQuery(query);
					if(rs.next()){
						rs.close();
						return true;
					}else{
						rs.close();
						return false;
					}			
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return false;
				}
			}catch(SQLException sqle){
				STUtils.writeErrorMessage(sqle.getMessage());
				return false;
			}
		}else{
			STUtils.writeDebugMessage("Connessione al database chiusa");
			return false;
		}
	}
	
	/**
	 * Verifica se nella tabella geometry_columns esiste già una riga contenente la tabella specificata
	 * @param tableName: nome della tabella
	 * @return true se la tabella è già stata aggiunta, false altrimenti
	 */
	public boolean geometryColumnsRowExists(String tableName){
		if(conn!=null){	
			ResultSet rs=null;		
			try{
				if(!conn.isClosed()){					
					Statement stmt = conn.createStatement();						
					String query = "select * from geometry_columns where f_table_name='" + tableName + "'" ;
					rs = stmt.executeQuery(query);
					if(rs.next()){
						rs.close();
						return true;
					}else{
						rs.close();
						return false;
					}			
				}else{
					STUtils.writeDebugMessage("Connessione al database chiusa");
					return false;
				}
			}catch(SQLException sqle){
				STUtils.writeErrorMessage(sqle.getMessage());
				return false;
			}
		}else{
			STUtils.writeDebugMessage("Connessione al database chiusa");
			return false;
		}
	}
	
	public void CloseConnection(){
		if(conn!=null){
			try{
				conn.close();
			}catch(Exception e){
				STUtils.writeDebugMessage("Errore durante la chiusura della connessione al database");
			}
		}
	}
}
