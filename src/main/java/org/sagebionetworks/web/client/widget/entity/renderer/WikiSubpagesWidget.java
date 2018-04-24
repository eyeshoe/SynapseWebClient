package org.sagebionetworks.web.client.widget.entity.renderer;

import static org.sagebionetworks.repo.model.EntityBundle.ENTITY;
import static org.sagebionetworks.repo.model.EntityBundle.PERMISSIONS;
import static org.sagebionetworks.web.client.ServiceEntryPointUtils.fixServiceEntryPoint;

import java.util.List;

import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.SynapseJavascriptClient;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.Wiki;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.utils.CallbackP;
import org.sagebionetworks.web.client.widget.entity.menu.v2.ActionMenuWidget;
import org.sagebionetworks.web.shared.WikiPageKey;
import org.sagebionetworks.web.shared.exceptions.NotFoundException;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class WikiSubpagesWidget implements IsWidget {
	
	private WikiSubpagesView view;
	private SynapseClientAsync synapseClient;
	private WikiPageKey wikiKey; 
	private String ownerObjectName;
	private Place ownerObjectLink;

	private boolean canEdit;
	private ActionMenuWidget actionMenu;
	
	//true if wiki is embedded in it's owner page.  false if it should be shown as a stand-alone wiki 
	private boolean isEmbeddedInOwnerPage;
	private CallbackP<WikiPageKey> reloadWikiPageCallback;
	private SynapseJavascriptClient jsClient;
	@Inject
	public WikiSubpagesWidget(WikiSubpagesView view, SynapseClientAsync synapseClient,
							AuthenticationController authenticationController,
							SynapseJavascriptClient jsClient) {
		this.view = view;		
		this.synapseClient = synapseClient;
		fixServiceEntryPoint(synapseClient);
		this.jsClient = jsClient;
	}

	public void configure(
			final WikiPageKey wikiKey, 
			boolean embeddedInOwnerPage, 
			CallbackP<WikiPageKey> reloadWikiPageCallback,
			ActionMenuWidget actionMenu) {
		
		//SWC-4177: if loading a page within the currently shown nav tree, don't reload the tree.
		if (this.wikiKey != null && 
			this.wikiKey.getOwnerObjectId().equals(wikiKey.getOwnerObjectId()) &&
			view.contains(wikiKey.getWikiPageId())
			) {
			view.setPage(wikiKey.getWikiPageId());
			view.showSubpages();
			return;
		}
		
		canEdit = false;
		this.reloadWikiPageCallback = reloadWikiPageCallback;
		this.wikiKey = wikiKey;
		this.isEmbeddedInOwnerPage = embeddedInOwnerPage;
		this.actionMenu = actionMenu;
		
		view.clear();
		view.hideSubpages();
		//figure out owner object name/link
		if (wikiKey.getOwnerObjectType().equalsIgnoreCase(ObjectType.ENTITY.toString())) {
			//lookup the entity name based on the id
			int mask = ENTITY | PERMISSIONS;
			jsClient.getEntityBundle(wikiKey.getOwnerObjectId(), mask, new AsyncCallback<EntityBundle>() {
				@Override
				public void onSuccess(EntityBundle bundle) {
					ownerObjectName = bundle.getEntity().getName();
					ownerObjectLink = getLinkPlace(bundle.getEntity().getId(), wikiKey.getVersion(), null, isEmbeddedInOwnerPage);
					canEdit = bundle.getPermissions().getCanEdit();
					refreshTableOfContents();
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage(caught.getMessage());
				}
			});
		}
	}
	
	public static Place getLinkPlace(String entityId, Long entityVersion, String wikiId, boolean isEmbeddedInOwnerPage) {
		if (isEmbeddedInOwnerPage)
			return new Synapse(entityId, entityVersion, Synapse.EntityArea.WIKI, wikiId);
		else
			return new Wiki(entityId, ObjectType.ENTITY.toString(), wikiId);
	}
	
	public Widget asWidget() {
		return view.asWidget();
	}
	
	public void refreshTableOfContents() {
		view.clear();
		synapseClient.getV2WikiHeaderTree(wikiKey.getOwnerObjectId(), wikiKey.getOwnerObjectType(), new AsyncCallback<List<V2WikiHeader>>() {
			@Override
			public void onSuccess(final List<V2WikiHeader> wikiHeaders) {
				synapseClient.getV2WikiOrderHint(wikiKey, new AsyncCallback<V2WikiOrderHint>() {
					@Override
					public void onSuccess(V2WikiOrderHint result) {
						// "Sort" stuff'
						WikiOrderHintUtils.sortHeadersByOrderHint(wikiHeaders, result);
						view.configure(wikiHeaders, ownerObjectName,
										ownerObjectLink, wikiKey, isEmbeddedInOwnerPage, reloadWikiPageCallback, actionMenu);
						view.setEditOrderButtonVisible(canEdit);
					}
					@Override
					public void onFailure(Throwable caught) {
						// Failed to get order hint. Just ignore it.
						view.configure(wikiHeaders, ownerObjectName,
								ownerObjectLink, wikiKey, isEmbeddedInOwnerPage, reloadWikiPageCallback, actionMenu);
						view.setEditOrderButtonVisible(canEdit);
					}
				});
			}
			
			@Override
			public void onFailure(Throwable caught) {
				//if this is because the wiki header root was not found, and the parent wiki id is null,
				if (caught instanceof NotFoundException) {
					//do nothing, show nothing
					view.clear();
				} else {
					view.showErrorMessage(caught.getMessage());
				}
			}
		});
	}
}
