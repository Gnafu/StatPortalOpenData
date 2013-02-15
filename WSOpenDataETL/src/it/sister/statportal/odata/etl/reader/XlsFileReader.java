package it.sister.statportal.odata.etl.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Lettore di un file Xls
 *
 */
public class XlsFileReader extends ExcelFileReader {

	public XlsFileReader(File file){
		super(file);
	}

	@Override
	protected Workbook getWorkbook(FileInputStream fileInputStream) throws IOException {
		return new HSSFWorkbook(fileInputStream);
	}
}
