package ch.specchio.db_import_export;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ch.specchio.constants.FileTypes;
import ch.specchio.eav_db.SQL_StatementBuilder;
import ch.specchio.file.writer.TableWriter;
import ch.specchio.file.writer.TableWriterFactory;


public class CampaignExport extends DbStructure {
		
	DbTable campaign_dbt;
	ArrayList<DbTable> table_list;
	
	TableWriter w;
	int campaign_id;
	
	
	public CampaignExport(SQL_StatementBuilder SQL, String schema, int campaign_id)
	{
		super(SQL, schema);
		
		this.campaign_id = campaign_id;	
		
		
	}
	
	public void export(OutputStream os) throws IOException
	{


		ThreadPoolExecutor exec = (ThreadPoolExecutor) Executors
				.newCachedThreadPool();

		Runnable  task = new Runnable()  {

			@Override
			public void run() {
				try {

					// create writer
					TableWriterFactory factory = new TableWriterFactory();
					w = factory.getWriter(os, FileTypes.CAMPAIGN_EXPORT_XML);

					w.write("<campaign>"); // proper xml document ....
					w.write_nl();

					DbTable campaign_dbt = get_table("campaign");  
					campaign_dbt.export(campaign_id, null); // start the export with the campaign, the rest is recursive

					w.write("</campaign>"); // proper xml document ....

					w.close();	            	


				} catch (Exception e) {
					e.printStackTrace();
				}


			}
		};

		exec.execute(task);
		
		try {
			exec.awaitTermination(1, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}


}
