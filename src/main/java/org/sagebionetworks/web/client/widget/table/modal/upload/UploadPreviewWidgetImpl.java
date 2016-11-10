package org.sagebionetworks.web.client.widget.table.modal.upload;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.Row;
import org.sagebionetworks.repo.model.table.UploadToTablePreviewResult;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * This widget shows a CSV upload preview generated by the services for a given file.
 * 
 * @author jhill
 *
 */
public class UploadPreviewWidgetImpl implements UploadPreviewWidget {

	public static final String NO_ROWS_WERE_FOUND_IN_THE_FILE = "No rows were found in the file.";
	public static final String NO_DATA_WAS_FOUND_IN_THE_FILE = "No data was found in the file.";
	public static final String PREVIEW_MESSAGE_PREFIX = "Scanned ";
	public static final String PREVIEW_MESSAGE_SUFFIX = " rows to generate preview:";
	public static final int MAX_CHARS_PER_CELL = 10;
	UploadPreviewView view;
	List<ColumnModel> columns;

	@Inject
	public UploadPreviewWidgetImpl(UploadPreviewView view) {
		super();
		this.view = view;
	}

	@Override
	public Widget asWidget() {
		return this.view.asWidget();
	}

	@Override
	public void configure(UploadToTablePreviewResult preview) {
		if(isPreviewSchemaEmpty(preview)){
			view.showEmptyPreviewMessage(NO_DATA_WAS_FOUND_IN_THE_FILE);
			view.setTableVisible(false);
			view.setEmptyMessageVisible(true);
			view.setPreviewMessage("");
		}else{
			// We at least have a header
			view.setTableVisible(true);
			view.setPreviewMessage(PREVIEW_MESSAGE_PREFIX+preview.getRowsScanned()+PREVIEW_MESSAGE_SUFFIX);
			columns = preview.getSuggestedColumns();
			// Create a list of headers
			List<String> headers = new ArrayList<String>();
			for(ColumnModel cm: preview.getSuggestedColumns()){
				StringBuilder builder = new StringBuilder();
				if(cm.getName() != null){
					builder.append(cm.getName());
				}else{
					builder.append("BLANK");
				}
				builder.append(" (");
				builder.append(cm.getColumnType().name());
				builder.append(")");
				headers.add(builder.toString());
			}
			view.setHeaders(headers);
			// rows
			if(isPreviewRowsEmpty(preview)){
				view.setEmptyMessageVisible(true);
				view.showEmptyPreviewMessage(NO_ROWS_WERE_FOUND_IN_THE_FILE);
			}else{
				// We have rows.
				view.setEmptyMessageVisible(false);
				for (Row row : preview.getSampleRows()) {
					List<String> values = new ArrayList<String>(row.getValues().size());
					for (String value : row.getValues()) {
						values.add(truncateValues(value));
					}
					view.addRow(values);
				}
			}
		}
	}
	
	/**
	 * Check for an empty preview.
	 * @param preview
	 * @return
	 */
	private boolean isPreviewRowsEmpty(UploadToTablePreviewResult preview){
		if(preview.getSampleRows() == null || preview.getSampleRows().isEmpty()){
			return true;
		}
		Row firstRow = preview.getSampleRows().get(0);
		if(firstRow == null || firstRow.getValues() == null || firstRow.getValues().isEmpty()){
			return true;
		}
		return false;
	}
	
	/**
	 * Is the preview schema empty.
	 * @param preview
	 * @return
	 */
	private boolean isPreviewSchemaEmpty(UploadToTablePreviewResult preview){
		if(preview == null){
			return true;
		}
		if(preview.getSuggestedColumns() == null || preview.getSuggestedColumns().isEmpty()){
			return true;
		}
		return false;
	}
	
	/**
	 * Truncate a string.
	 * @param in
	 * @return
	 */
	public static String truncateValues(String in){
		if(in == null){
			return null;
		}
		if(in.length() > MAX_CHARS_PER_CELL){
			return in.substring(0, MAX_CHARS_PER_CELL-1)+"...";
		}
		return in;
	}
}
