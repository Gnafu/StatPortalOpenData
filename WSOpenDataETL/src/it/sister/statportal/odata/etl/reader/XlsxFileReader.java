package it.sister.statportal.odata.etl.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Lettore di un file Xlsx
 *
 */
public class XlsxFileReader extends ExcelFileReader {

	public XlsxFileReader(File file){
		super(file);
	}

	@Override
	protected Workbook getWorkbook(FileInputStream fileInputStream)
			throws IOException {
		return new XSSFWorkbook(fileInputStream);
	}
}
