package ch.specchio.file.reader.spectrum;

import java.io.File;
import java.io.IOException;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import jxl.Cell;
import jxl.NumberCell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.file.reader.campaign.SpecchioCampaignDataLoader;
import ch.specchio.types.SpectralFile;

public class XLS_FileLoader extends SpectralFileLoader {

	private int c;
	private int r;

	public XLS_FileLoader(SPECCHIOClient specchio_client, SpecchioCampaignDataLoader campaignDataLoader) {
		super("XLS", specchio_client, campaignDataLoader);
	}

	@Override
	public SpectralFile load(File file) throws IOException {
		
		SpectralFile f = null;
		
		// read xls file
		try {
			Workbook w = Workbook.getWorkbook(file);			
			Sheet sheet = w.getSheet(0);
			f = new SpectralFile();
			
			f.setFilename(file.getName());
			f.setFileFormatName(this.file_format_name);
			f.setCompany(""); // unknown sensor company
			
			f.setNumberOfSpectra(sheet.getColumns() - 1); 	// first column is wavelength	
			
			// get spectrum file names
			// set capture dates: equal to loading date
//			TimeZone tz = TimeZone.getTimeZone("UTC");
//			Calendar cal = Calendar.getInstance(tz);
			for(c = 1;c<sheet.getColumns();c++)
			{
				f.addSpectrumFilename(file.getName() + "_" + sheet.getCell(c, 0).getContents());
				f.setCaptureDate(c-1, new DateTime(DateTimeZone.UTC));	
			}
			
			// get wvls
			Float[] wvls = new Float[sheet.getRows()-1];
			
			c = 0;
			Cell[] col1 = sheet.getColumn(c);
			
			for(r=1;r<sheet.getRows();r++)
			{
				NumberCell nc = (NumberCell) col1[r];
				Double tmp = nc.getValue();					
				wvls[r-1] = tmp.floatValue();
			}
			
			f.addNumberOfChannels(wvls.length);
			
			// get spectra
			Float[][] spectra = new Float[sheet.getColumns()-1][sheet.getRows()-1];
			
			for(c = 1;c<sheet.getColumns();c++)
			{
				Cell[] col = sheet.getColumn(c);
				for(r=1;r<sheet.getRows();r++)
				{
					NumberCell nc = (NumberCell) col[r];
					Double tmp = nc.getValue();
					spectra[c-1][r-1] = tmp.floatValue();
				}
				
				f.addWvls(wvls); // add wvls for each spectrum
			}
			
			f.setMeasurements(spectra);
			
			
		} catch (BiffException ex) {
			throw new IOException(ex);
		} catch (NumberFormatException ex) {
			throw new IOException("Invalid number format (" + ex.getMessage() + ") at column " + (c+1) + ", row " + (r+1), ex);
		}
		catch (ArrayIndexOutOfBoundsException ex) {
			throw new IOException("Wrongly formatted file (e.g. empty rows at end of file) (" + ex.getMessage() + ") at column " + (c+1) + ", row " + (r+1), ex);
		}	
		catch (java.lang.ClassCastException ex) {
			throw new IOException("Wrongly formatted file (" + ex.getMessage() + ") at column " + (c+1) + ", row " + (r+1), ex);
		}			
		
		return f;
	}

}
