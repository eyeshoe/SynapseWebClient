package org.sagebionetworks.web.client.widget.accessrequirements;

import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.BlockQuote;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.html.Div;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.utils.Callback;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class ACTAccessRequirementWidgetViewImpl implements ACTAccessRequirementWidgetView {

	@UiField
	Div approvedHeading;
	@UiField
	Div unapprovedHeading;
	@UiField
	BlockQuote wikiTermsUI;
	@UiField
	SimplePanel wikiContainer;
	@UiField
	BlockQuote termsUI;
	@UiField
	HTML terms;
	@UiField
	Button requestAccessButton;
	@UiField
	Div editAccessRequirementContainer;
	@UiField
	Div deleteAccessRequirementContainer;
	
	@UiField
	Div subjectsWidgetContainer;
	@UiField
	Div synAlertContainer;
	@UiField
	Div requestAccessButtonContainer;
	@UiField
	Alert requestApprovedMessage;

	Callback onAttachCallback;
	public interface Binder extends UiBinder<Widget, ACTAccessRequirementWidgetViewImpl> {
	}
	
	Widget w;
	Presenter presenter;
	
	@Inject
	public ACTAccessRequirementWidgetViewImpl(Binder binder){
		this.w = binder.createAndBindUi(this);
		requestAccessButton.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				presenter.onRequestAccess();
			}
		});
		w.addAttachHandler(new AttachEvent.Handler() {
			
			@Override
			public void onAttachOrDetach(AttachEvent event) {
				if (event.isAttached()) {
					onAttachCallback.invoke();
				}
			}
		});
	}
	
	@Override
	public void addStyleNames(String styleNames) {
		w.addStyleName(styleNames);
	}
	
	@Override
	public void setPresenter(final Presenter presenter) {
		this.presenter = presenter;
	}
	
	@Override
	public Widget asWidget() {
		return w;
	}
	@Override
	public void setWikiTermsWidget(Widget wikiWidget) {
		wikiContainer.setWidget(wikiWidget);
	}
	@Override
	public void setTerms(String arText) {
		terms.setHTML(arText);
	}
	@Override
	public void showTermsUI() {
		termsUI.setVisible(true);
	}
	@Override
	public void showWikiTermsUI() {
		wikiTermsUI.setVisible(true);
	}
	@Override
	public void showApprovedHeading() {
		approvedHeading.setVisible(true);
	}
	@Override
	public void showUnapprovedHeading() {
		unapprovedHeading.setVisible(true);
	}
	
	@Override
	public void showRequestAccessButton() {
		requestAccessButton.setVisible(true);
	}
	
	@Override
	public void resetState() {
		approvedHeading.setVisible(false);
		unapprovedHeading.setVisible(false);
		requestAccessButton.setVisible(false);
		requestApprovedMessage.setVisible(false);
	}
	
	@Override
	public void setEditAccessRequirementWidget(IsWidget w) {
		editAccessRequirementContainer.clear();
		editAccessRequirementContainer.add(w);
	}
	@Override
	public void setDeleteAccessRequirementWidget(IsWidget w) {
		deleteAccessRequirementContainer.clear();
		deleteAccessRequirementContainer.add(w);
	}
	@Override
	public void setSubjectsWidget(IsWidget w) {
		subjectsWidgetContainer.clear();
		subjectsWidgetContainer.add(w);
	}
	@Override
	public void setVisible(boolean visible) {
		w.setVisible(visible);
	}
	@Override
	public void setSynAlert(IsWidget w) {
		synAlertContainer.clear();
		synAlertContainer.add(w);
	}
	@Override
	public void setOnAttachCallback(Callback onAttachCallback) {
		this.onAttachCallback = onAttachCallback;
	}
	@Override
	public boolean isInViewport() {
		return DisplayUtils.isInViewport(w);
	}
	@Override
	public boolean isAttached() {
		return w.isAttached();
	}
	
	@Override
	public void hideButtonContainers() {
		editAccessRequirementContainer.setVisible(false);
		deleteAccessRequirementContainer.setVisible(false);
		requestAccessButtonContainer.setVisible(false);
	}
	@Override
	public void showRequestApprovedMessage() {
		requestApprovedMessage.setVisible(true);
	}
}
