package it.sister.extension.mapImage;

public class ImageLayer{
		
		private String imagePath;
		private int alpha;
		private String url;
		private RequestType requestType;
		
		public ImageLayer(String imagePath, int alpha){
			this.imagePath = imagePath;
			this.alpha = alpha;
		}
		
		public ImageLayer(String url, RequestType requestType, int alpha){
			this.url = url;
			this.requestType = requestType;
			this.alpha = alpha;
		}
		
		public String getImagePath(){
			return imagePath;
		}
		
		public void setPath(String imagePath){
			this.imagePath = imagePath;
		}
		
		public int getAlpha(){
			return alpha;
		}
		
		public String getBasicUrl(){
			String[] splittedUrl = url.split("\\?");
			return splittedUrl[0];
		}
		
		public String getCompleteUrl(){
			return url;
		}
		
		public String getParameters(){
			String[] splittedUrl = url.split("\\?");
			if(splittedUrl.length != 2){
				return null;
			}
			return splittedUrl[1];
		}
		
		public RequestType getRequestType(){
			return requestType;
		}				
	}
