package org.sagebionetworks.web.client.presenter;

import static org.sagebionetworks.web.client.ServiceEntryPointUtils.fixServiceEntryPoint;

import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.quiz.PassingRecord;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.view.CertificateView;
import org.sagebionetworks.web.client.widget.entity.controller.SynapseAlert;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class CertificatePresenter extends AbstractActivity implements CertificateView.Presenter, Presenter<org.sagebionetworks.web.client.place.Certificate> {

	private CertificateView view;
	private GlobalApplicationState globalApplicationState;
	private SynapseClientAsync synapseClient;
	private AdapterFactory adapterFactory;
	private SynapseAlert synAlert;
	private SynapseJavascriptClient jsClient;
	
	@Inject
	public CertificatePresenter(CertificateView view,  
			GlobalApplicationState globalApplicationState,
			SynapseClientAsync synapseClient,
			SynapseJavascriptClient jsClient,
			AdapterFactory adapterFactory,
			SynapseAlert synAlert){
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		fixServiceEntryPoint(synapseClient);
		this.synapseClient = synapseClient;
		this.jsClient = jsClient;
		this.adapterFactory = adapterFactory;
		this.synAlert = synAlert;
		this.view.setPresenter(this);
		view.setSynapseAlertWidget(synAlert.asWidget());
	}
	
	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		panel.setWidget(this.view.asWidget());
	}

	
	@Override
	public void goTo(Place place) {
		globalApplicationState.getPlaceChanger().goTo(place);
	}
	
	@Override
	public void goToLastPlace() {
		view.hideLoading();
		globalApplicationState.gotoLastPlace();
	}
	
	@Override
    public String mayStop() {
        view.clear();
        return null;
    }

	@Override
	public void setPlace(org.sagebionetworks.web.client.place.Certificate place) {
		view.setPresenter(this);
		initStep1(place.toToken());
	}
	
	public void initStep1(final String principalId) {
		synAlert.clear();
		view.clear();
		view.showLoading();
		jsClient.getUserProfile(principalId, new AsyncCallback<UserProfile>() {
			@Override
			public void onSuccess(UserProfile profile) {
				initStep2(principalId, profile);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.hideLoading();
				synAlert.handleException(caught);
			}
		});
	}
	
	public void initStep2(String principalId, final UserProfile profile) {
		synapseClient.getCertifiedUserPassingRecord(principalId, new AsyncCallback<String>() {
			@Override
			public void onSuccess(String passingRecordJson) {
				try {
					//if certified, show the certificate
					PassingRecord passingRecord = new PassingRecord(adapterFactory.createNew(passingRecordJson));
					view.showSuccess(profile, passingRecord);
				} catch (JSONObjectAdapterException e) {
					onFailure(e);
				}
			}
			@Override
			public void onFailure(Throwable caught) {
				if (caught instanceof NotFoundException) {
					//show user is not certified
					view.showNotCertified(profile);
				} else {
					view.hideLoading();
					synAlert.handleException(caught);
				}
			}
		});

	}
	
	@Override
	public void okButtonClicked() {
		goToLastPlace();
	}
}
