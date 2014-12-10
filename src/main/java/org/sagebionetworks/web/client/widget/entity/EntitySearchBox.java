package org.sagebionetworks.web.client.widget.entity;

import java.util.List;

import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.AdapterFactory;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.shared.PaginatedResults;
import org.sagebionetworks.web.shared.SearchQueryUtils;
import org.sagebionetworks.web.shared.exceptions.UnknownErrorException;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

/**
 * This widget is a Synapse entity Id search box
 * 
 * @author Dburdick
 *
 */
public class EntitySearchBox implements EntitySearchBoxView.Presenter, IsWidget {
	
	private EntitySearchBoxView view;
	private EntitySelectedHandler handler;
	private SynapseClientAsync synapseClient;
	private NodeModelCreator nodeModelCreator;

	private boolean retrieveVersions = false;
	
	/**
	 * 
	 * @param factory
	 * @param cache
	 * @param propertyView
	 */
	@Inject
	public EntitySearchBox(AdapterFactory factory, EntitySearchBoxView view,
			SynapseClientAsync synapseClient, NodeModelCreator nodeModelCreator,
			GlobalApplicationState globalApplicationState,
			AuthenticationController authenticationController) {
		super();		
		this.view = view;
		this.synapseClient = synapseClient;
		this.nodeModelCreator = nodeModelCreator;
		view.setPresenter(this);
	}

	/**
	 * Get widget with text box of specified width
	 * @param width the width of the input box
	 * @return
	 */	
	public Widget asWidget(int width) {
		this.view.build(width);
		return this.view.asWidget();
	}

	/**
	 * Default size
	 * @param DIALOG_WIDTH
	 * @return
	 */
	@Override
	public Widget asWidget() {
		return asWidget(300);
	}
	
	public void setEntitySelectedHandler(EntitySelectedHandler handler, boolean retrieveVersions) {
		this.handler = handler;		
		this.retrieveVersions = retrieveVersions;		
	}

	@Override
	public void entitySelected(final String entityId, final String name) {
		if(handler != null) {
			List<VersionInfo> versions = null;
			if(retrieveVersions) {
				synapseClient.getEntityVersions(entityId, 1, 20, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String result) {
						PaginatedResults<VersionInfo> versions;
						try {
							versions = nodeModelCreator.createPaginatedResults(result, VersionInfo.class);
							handler.onSelected(entityId, name, versions.getResults());
						} catch (JSONObjectAdapterException e) {
							onFailure(new UnknownErrorException(DisplayConstants.ERROR_INCOMPATIBLE_CLIENT_VERSION));
						}
					}
					@Override
					public void onFailure(Throwable caught) {
						view.showErrorMessage(DisplayConstants.ERROR_GENERIC);
					}
				});
			} else {				
				handler.onSelected(entityId, name, versions);
			}
		}
	}

	public interface EntitySelectedHandler {
		public void onSelected(String entityId, String name, List<VersionInfo> versions);
	}

	@Override
	public void search(String search) {
		SearchQuery query = SearchQueryUtils.getDefaultSearchQuery();
		
	}
	
	/**
	 * Clears out the state of the searchbox
	 */
	public void clearSelection() {
		this.view.clearSelection();
	}
}
