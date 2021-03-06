package org.sagebionetworks.web.client.presenter;


import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.ENTITY_PATH;

import java.util.List;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Link;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GWTWrapper;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.events.EntityUpdatedEvent;
import org.sagebionetworks.web.client.events.EntityUpdatedHandler;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.Callback;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.view.EntityView;
import org.sagebionetworks.web.client.widget.entity.EntityPageTop;
import org.sagebionetworks.web.client.widget.entity.controller.StuAlert;
import org.sagebionetworks.web.client.widget.header.Header;
import org.sagebionetworks.web.client.widget.team.OpenTeamInvitationsWidget;
import org.sagebionetworks.web.shared.OpenUserInvitationBundle;
import org.sagebionetworks.web.shared.exceptions.ForbiddenException;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EntityPresenter extends AbstractActivity implements EntityView.Presenter, Presenter<Synapse>, IsWidget {
		
	private Synapse place;
	private EntityView view;
	private GlobalApplicationState globalApplicationState;
	private AuthenticationController authenticationController;
	private StuAlert synAlert;
	private String entityId;
	private Long versionNumber;
	private Synapse.EntityArea area;
	private String areaToken;
	private CookieProvider cookies;
	private Header headerWidget;
	private EntityPageTop entityPageTop;
	private OpenTeamInvitationsWidget openTeamInvitesWidget;
	private GWTWrapper gwt;
	private SynapseJavascriptClient jsClient;
	@Inject
	public EntityPresenter(EntityView view,
			GlobalApplicationState globalAppState,
			AuthenticationController authenticationController,
			SynapseJavascriptClient jsClient, CookieProvider cookies,
			StuAlert synAlert,
			EntityPageTop entityPageTop, Header headerWidget,
			OpenTeamInvitationsWidget openTeamInvitesWidget,
			GWTWrapper gwt
			) {
		this.headerWidget = headerWidget;
		this.entityPageTop = entityPageTop;
		this.openTeamInvitesWidget = openTeamInvitesWidget;
		this.view = view;
		this.synAlert = synAlert;
		this.globalApplicationState = globalAppState;
		this.authenticationController = authenticationController;
		this.jsClient = jsClient;
		this.cookies = cookies;
		this.gwt = gwt;
		clear();
		entityPageTop.setEntityUpdatedHandler(new EntityUpdatedHandler() {			
			@Override
			public void onPersistSuccess(EntityUpdatedEvent event) {
				//reload current window
				//get the place based on the current url
				globalApplicationState.refreshPage();
			}
		});
		
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		clear();
		// Install the view
		panel.setWidget(view);
		view.setLoadingVisible(true);
	}

	@Override
	public void setPlace(Synapse place) {
		this.place = place;
		this.entityId = place.getEntityId();
		this.versionNumber = place.getVersionNumber();
		this.area = place.getArea();
		this.areaToken = place.getAreaToken();
		refresh();
	}
	
	public static boolean isValidEntityId(String entityId) {
		if (entityId == null || entityId.length() == 0 || !entityId.toLowerCase().startsWith("syn")) {
			return false;
		}
		
		//try to parse the actual syn id
		try {
			Long.parseLong(entityId.substring("syn".length()).trim());
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
	
	@Override
	public void clear() {
		entityPageTop.clearState();
		synAlert.clear();
		openTeamInvitesWidget.clear();
		view.clear();
		view.setAccessDependentMessageVisible(false);
	}
	
	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
	@Override
	public void refresh() {
		clear();
		headerWidget.refresh();
		//place widgets and configure
		view.setEntityPageTopWidget(entityPageTop);
		view.setOpenTeamInvitesWidget(openTeamInvitesWidget);
		view.setSynAlertWidget(synAlert.asWidget());
		// Hide the view panel contents until async callback completes
		view.setLoadingVisible(true);
		int mask = ENTITY | ENTITY_PATH;
		final AsyncCallback<EntityBundle> callback = new AsyncCallback<EntityBundle>() {
			@Override
			public void onSuccess(EntityBundle bundle) {
				view.setLoadingVisible(false);
				// Redirect if Entity is a Link
				if(bundle.getEntity() instanceof Link) {
					Reference ref = ((Link)bundle.getEntity()).getLinksTo();
					entityId = null;
					if(ref != null){
						// redefine where the page is and refresh
						entityId = ref.getTargetId();
						versionNumber = ref.getTargetVersionNumber();
						refresh();
						return;
					} else {
						// show error and then allow entity bundle to go to view
						view.showErrorMessage(DisplayConstants.ERROR_NO_LINK_DEFINED);
					}
				}
				EntityHeader projectHeader = DisplayUtils.getProjectHeader(bundle.getPath());
				if(projectHeader == null) {
					synAlert.showError(DisplayConstants.ERROR_GENERIC_RELOAD);
				} else {
					entityPageTop.configure(bundle.getEntity(), versionNumber, projectHeader, area, areaToken);
					view.setEntityPageTopWidget(entityPageTop);
					view.setEntityPageTopVisible(true);
					headerWidget.configure(projectHeader);	
				}
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.setLoadingVisible(false);
				headerWidget.configure();
				if(caught instanceof NotFoundException) {
					show404();
				} else if(caught instanceof ForbiddenException && authenticationController.isLoggedIn()) {
					show403();
				} else {
					view.clear();
					synAlert.handleException(caught);
				}
			}			
		};
		
		if(isValidEntityId(entityId)) {
			if (versionNumber == null) {
				jsClient.getEntityBundle(entityId, mask, callback);
			} else {
				jsClient.getEntityBundleForVersion(entityId, versionNumber, mask, callback);
			}
		} else {
			//invalid entity detected, indicate that the page was not found
			gwt.scheduleDeferred(new Callback() {
				@Override
				public void invoke() {
					callback.onFailure(new NotFoundException());		
				}
			});
		}
	}
	
	public void show403() {
		if (entityId != null) {
			synAlert.show403(entityId);
		}
		view.setLoadingVisible(false);
		view.setEntityPageTopVisible(false);
		//also add the open team invitations widget (accepting may gain access to this project)
		openTeamInvitesWidget.configure(new Callback() {
			@Override
			public void invoke() {
				//when team is updated, refresh to see if we can now access
				refresh();
			}
			
		}, new CallbackP<List<OpenUserInvitationBundle>>() {

			@Override
			public void invoke(List<OpenUserInvitationBundle> invites) {
				//if there are any, then also add the title text to the panel
				if (invites != null && invites.size() > 0) {
					view.setAccessDependentMessageVisible(true);
				}
			}
			
		});
		view.setOpenTeamInvitesVisible(true);
	}
	
	public void show404() {
		synAlert.show404();
		view.setLoadingVisible(false);
		view.setEntityPageTopVisible(false);
		view.setOpenTeamInvitesVisible(false);
	}
	
	@Override
	public Widget asWidget() {
		return view.asWidget();
	}
	
	// for testing only
	
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
}
