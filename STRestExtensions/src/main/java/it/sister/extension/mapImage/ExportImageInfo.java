package it.sister.extension.mapImage;

public class ExportImageInfo {

	private String fileName;
	private int width;
	private int height;
	private ImageLayer[] layers;
	
	public ExportImageInfo(String fileName, int width, int height, ImageLayer[] layers){
		this.fileName = fileName;
		this.width = width;
		this.height = height;
		this.layers = layers;
	}
	
	public String getFileName(){
		return fileName;
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
	
	public ImageLayer[] getLayers(){
		return layers;
	}	
	
}
