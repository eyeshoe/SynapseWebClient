package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.utils.Callback;

public interface PopupUtilsView {
	void showInfo(String message);
	void showError(String message, Integer timeout);
	void showErrorMessage(String message);
	void showErrorMessage(String title, String message);
	void showInfoDialog(
			String title, 
			String message,
			Callback okCallback
			);
	void showConfirmDialog(
			String title, 
			String message,
			Callback yesCallback,
			Callback noCallback
			);
	void showConfirmDialog(
			String title, 
			String message,
			Callback yesCallback
			);
	void openInNewWindow(String url);
	void showConfirmDelete(String message, 
			Callback callback);
}
