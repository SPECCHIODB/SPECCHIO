package ch.specchio.metadata;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import ch.specchio.client.SPECCHIOClient;
import ch.specchio.client.SPECCHIOClientException;
import ch.specchio.client.SPECCHIOWebClientException;
import ch.specchio.gui.MetaDataEditorView;
import ch.specchio.gui.SpectrumMetadataCategoryList;
import ch.specchio.types.Campaign;
import ch.specchio.types.ConflictTable;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.MetaParameterFormatException;
import ch.specchio.types.MetaTaxonomy;
import ch.specchio.types.Metadata;
import ch.specchio.types.MetadataInterface;
import ch.specchio.types.TaxonomyNodeObject;
import ch.specchio.types.attribute;

public abstract class MDE_Controller implements MD_ChangeListener {
	
	Metadata eav_md;
	MDE_FormFactory form_factory;
	ArrayList<Integer> ids;
	Campaign campaign;
	SPECCHIOClient specchio_client;
	MDE_Form form;
	MD_FormDescriptor form_descriptor;
	Hashtable<Integer, attribute> attributes;
	Boolean do_conflict_detection = true;
	protected MetadataInterface first_entry;
	ConflictTable eav_conflict_stati;
	ConflictTable spectrum_md_conflict_stati;
	MetaDataEditorView mdev;
	private SpectrumMetadataCategoryList category_list;
	protected boolean onlyHierarchiesAreSelected = true;
	protected int metadata_level;
	
	ArrayList<MD_Field> changed_fields = new ArrayList<MD_Field>();
	ArrayList<MD_Field> removed_fields = new ArrayList<MD_Field>();
	ArrayList<MD_Field> added_fields = new ArrayList<MD_Field>();
	ArrayList<MD_Field> changed_annotations = new ArrayList<MD_Field>();
	
	
	public MDE_Controller(SPECCHIOClient specchio_client) throws SPECCHIOClientException
	{
		this(specchio_client, null);
	}
	
	public MDE_Controller(SPECCHIOClient specchio_client, MetaDataEditorView metaDataEditorView) throws SPECCHIOClientException
	{
		this.form_factory = new MDE_FormFactory(specchio_client);
		this.specchio_client = specchio_client;
		this.attributes = specchio_client.getAttributesIdHash();
		set_form_descriptor(this.form_factory.getDefaultFormDescriptor(), false);
		this.mdev = metaDataEditorView;
	}
	
	
	public Campaign getCampaign()
	{
		return campaign;
	}
	
	
	public ArrayList<Integer> getIds()
	{	
		return ids;
		
	}
	
	
	public MDE_FormFactory getFormFactory()
	{
		return form_factory;
	}
	
	
	public ArrayList<MD_Field> getChanged_fields() {
		return changed_fields;
	}
	
	public ArrayList<MD_Field> getChanged_fields(int level) {

		ArrayList<MD_Field> fields = new ArrayList<MD_Field>();
		for (MD_Field field : getChanged_fields()) {
			if(field.getLevel() == level)
				fields.add(field);
		}

		return fields;
		
	}			

	public ArrayList<MD_Field> getRemoved_fields() {
		return removed_fields;
	}
	
	/**
	 * Get removed fields of a metaparameter storage level
	 * @param level
	 * @return parameter list of required level
	 */
	public ArrayList<MD_Field> getRemoved_fields(int level) {
		
		ArrayList<MD_Field> removed_fields_of_level = new ArrayList<MD_Field>();
		
		for(MD_Field f : removed_fields)
		{
			if(f.getLevel() == level) removed_fields_of_level.add(f);
		}		
		
		return removed_fields_of_level;
	}	

	public ArrayList<MD_Field> getAdded_fields() {
		return added_fields;
	}

	public ArrayList<MD_Field> getChanged_annotations() {
		return changed_annotations;
	}

	public void set_campaign(Campaign campaign)
	{
		this.campaign = campaign;
	}
	
	
	public Boolean getDo_conflict_detection() {
		return do_conflict_detection;
	}


	public void setDo_conflict_detection(Boolean do_conflict_detection) {
		this.do_conflict_detection = do_conflict_detection;
	}


	public void set_form_descriptor(MD_FormDescriptor form_descriptor, boolean manual_category_selection) throws SPECCHIOClientException
	{
		this.form_descriptor = form_descriptor;	
		update_form(manual_category_selection);
	}
	
	
	abstract public ArrayList<Integer> get_hierarchy_ids();		
	
	public boolean getOnlyHierarchiesAreSelected() {
		return this.onlyHierarchiesAreSelected;		
	}	
	
	public int getMetadataLevel()
	{
		return this.metadata_level;
	}
	
	protected void update_form(boolean manual_category_selection) throws SPECCHIOClientException
	{
		if (ids != null && ids.size() > 0 && first_entry.getMetadata() != null)
		{
			
			
			// check if the category list of the container must be updated
			if(category_list != null)
			{
				MetaTaxonomy ap_domain = (MetaTaxonomy) this.first_entry.getMetadata().get_first_entry("Application Domain");

				if(ap_domain != null && !manual_category_selection) // avoid applying the application domain specific selection of categories if user manually interacted
				{

					// update the categories according to the application domain
					ArrayList<Integer> selected_categories = null;
					Long taxonomy_id =  (Long) ap_domain.getValue();

					//SpectrumMetadataCategoryList category_list = this.mdev.getCategory_list();

					selected_categories = specchio_client.getMetadataCategoriesForApplicationDomain(taxonomy_id.intValue());
					if(selected_categories.size()>0)
					{
						category_list.setSelected(selected_categories);
						TaxonomyNodeObject tmp = specchio_client.getTaxonomyNode(taxonomy_id.intValue());
						category_list.setApplicationDomain(tmp.getName());
					}
					else
					{
						category_list.setAllSelected(true);
						category_list.setApplicationDomain(null);
					}

					this.form_descriptor = category_list.getFormDescriptor();	

				}
				
				if(ap_domain == null && !manual_category_selection)
				{
					
					// enable all categories
					if(category_list.isApplicationDomainEnabled())
					{
						category_list.setAllSelected(true);
						category_list.setApplicationDomain(null);
						//set_form_descriptor(category_list.getFormDescriptor(), false);
					}
	
					
				}				
			}



			// get form			
			create_form();
			
			Enumeration<String> conflicts = eav_conflict_stati.conflicts();
			while (conflicts.hasMoreElements()) {
				try {
					
					int attribute_id = Integer.parseInt(conflicts.nextElement());
					List<MetaParameter> mps = this.first_entry.getMetadata().get_all_entries(attribute_id);
										
					if (mps.size() > 0) {
						
						// add a field for every instance of this attribute
						for (MetaParameter mp : mps) {
							form.addEavParameterIntoExistingContainer(mp, eav_conflict_stati.get(attribute_id));
						}
						
					} else {
						
						// the first spectrum doesn't have this metadata, so it must be in conflict and we can use an empty metaparameter
						MetaParameter mp = MetaParameter.newInstance(attributes.get(attribute_id));
						mp.setEavId(eav_conflict_stati.get(attribute_id).eavIds().nextElement());
						form.addEavParameterIntoExistingContainer(mp, eav_conflict_stati.get(attribute_id));
					}
					
				}
				catch (NumberFormatException ex) {
					// the server returned a non-numeric attribute id
					throw new SPECCHIOClientException(ex);
				}
			}

			//form.addParametersIntoExistingContainers(this.first_entry, Spectrum.METADATA_FIELDS, spectrum_md_conflict_stati);
			
		} else {
			form = null;
		}
		
	}
	
	
	
	abstract void create_form();
	
	
	public void clear_changed_field_lists()
	{
		changed_fields.clear();
		removed_fields.clear();
		added_fields.clear();		
		changed_annotations.clear();
	}

	public MDE_Form getForm()
	{
		return form;
	}

	public boolean hasChanges()
	{
		return this.changed_fields.size()>0 || this.removed_fields.size()>0 || this.changed_annotations.size()>0;
	}

	public void update(ArrayList<MD_Field> changed_fields) throws SPECCHIOClientException {
		
		ListIterator<MD_Field> li = changed_fields.listIterator();
		
		while(li.hasNext())
		{
			MD_Field field = li.next();			
			update(field);						
		}
	}
	
	
	
	public void update(MD_Field field) throws SPECCHIOClientException
	{
		
		if(field instanceof MD_EAV_Field)
		{
			try {
				MD_EAV_Field eav_field = (MD_EAV_Field) field;
				MetaParameter mp = eav_field.getMetaParameter();
				
				mp.setValue(field.getNewValue());

				int old_eav_id = mp.getEavId();
				
				// decide if the update is done on spectrum or hierarchy level
				ArrayList<Integer> ids_to_update;
				if(this.onlyHierarchiesAreSelected)
				{
					ids_to_update = this.get_hierarchy_ids();
					mp.setLevel(MetaParameter.HIERARCHY_LEVEL);
				}
				else
					ids_to_update = this.getIds();
				
				int eav_id = specchio_client.updateEavMetadata(mp, ids_to_update);
				eav_field.setEavId(eav_id, old_eav_id);					


			}
			catch (MetaParameterFormatException ex) {
				// should never happen
				ex.printStackTrace();
			}
			
		}	
		
		if(field instanceof MD_Spectrum_Field)
		{
			MD_Spectrum_Field spectrum_field = (MD_Spectrum_Field)field;
			spectrum_field.setId((Integer) field.getNewValue());
			specchio_client.updateSpectraMetadata(ids, spectrum_field.db_field_name, spectrum_field.getId());			
		}
		
		
	}
	
	
	public void updateAnnotation(MD_Field field) throws SPECCHIOWebClientException {
		
		MD_EAV_Field eav_field = (MD_EAV_Field) field;
		MetaParameter mp = eav_field.getMetaParameter();
		
		mp.setAnnotation(field.getAnnotation());
		
		int eav_id = specchio_client.updateEavMetadataAnnotation(mp, ids);
		
		
	}
	
	
	public void updateCampaign(Campaign campaign) throws SPECCHIOClientException {
		
		this.campaign = campaign;
		specchio_client.updateCampaign(campaign);
		
	}


	public void update_selection(MD_Field field) throws SPECCHIOClientException {
		// this is the case when only a sub-set of a spectral collection are to be assigned a new value
		// the following is done to retain the old value for the non-selected spectra
		// - create a new EAV entry by substituting the value and setting the eav_id to zero
		// - link new entry with spectral subset
		// - remove links to old EAV for spectral subset
		
		if(field instanceof MD_EAV_Field)
		{
			try {
				MD_EAV_Field eav_field = (MD_EAV_Field) field;
				MetaParameter mp = eav_field.getMetaParameter();
				mp.setValue(field.getNewValue());
				int old_eav_id = mp.getEavId();
				int eav_id = specchio_client.updateEavMetadata(mp, ids, mp);
				eav_field.setEavId(eav_id, old_eav_id);
			}
			catch (MetaParameterFormatException ex) {
				// should never happen
				ex.printStackTrace();
			}
		}
	}
	
	public void remove(MD_Field field) throws SPECCHIOClientException
	{
		
		if(field instanceof MD_EAV_Field)
		{
			MD_EAV_Field eav_field = (MD_EAV_Field) field;
			MetaParameter mp = eav_field.getMetaParameter();
			specchio_client.removeEavMetadata(mp);
		}
		if (form != null) {
			form.removeField(field);
		}
	}
	
	abstract public void remove_selection(MD_Field field) throws SPECCHIOClientException;



	public void remove_all_mps_of_attribute(MD_Field field) throws SPECCHIOClientException {
		
		MetaParameter mp = ((MD_EAV_Field) field).getMetaParameter();
		attribute attr = specchio_client.getAttributesIdHash().get(mp.getAttributeId());
		specchio_client.removeEavMetadata(attr, ids, field.getLevel());
		if (form != null) {
			form.removeField(field);
		}
	}
	
	public void remove_all_added_fields()
	{
		ListIterator<MD_Field> li = getAdded_fields().listIterator();
		
		while(li.hasNext())
		{
			MD_Field field = li.next();
			form.removeField(field);
		}
		
	}

	public void setCategoryList(SpectrumMetadataCategoryList category_list) {
		this.category_list = category_list;
		
	}


	public void metadataFieldAdded(MD_Field field)
	{
		this.added_fields.add(field);
	}
	
	
	public void metadataFieldChanged(MD_Field field, Object new_value)
	{	
		field.setNewValue(new_value);
		
		if(!changed_fields.contains(field))
		{
			changed_fields.add(field);
		}	
	}
	
	public void metadataFieldRemoved(MD_Field field)
	{
				
		if(!removed_fields.contains(field))
		{
			removed_fields.add(field);
		}	
	}
	

	public void metadataFieldAnnotationChanged(MD_Field field, String annotation) {
		
		field.setAnnotation(annotation);
		
		if(!changed_annotations.contains(field))
		{
			changed_annotations.add(field);
		}	
		
	}

	public int clearRemoved_fields(int level) {

		int number_of_removed_fields = removed_fields.size();

		// use iterator to remove fields from list
		Iterator<MD_Field> it = removed_fields.iterator();
		
		while(it.hasNext())
		{
			MD_Field f = it.next();
			if(f.getLevel() == level) it.remove();
		}				

		return number_of_removed_fields;
	}
	
	
	public void clearChanged_fields(int level) {
		
		// use iterator to remove fields from list
		Iterator<MD_Field> it = this.changed_fields.iterator();
		
		while(it.hasNext())
		{
			MD_Field f = it.next();
			if(f.getLevel() == level) it.remove();
		}				
		
	}

	public void clearAdded_fields(int level) {
		// use iterator to remove fields from list
		Iterator<MD_Field> it = this.added_fields.iterator();
		
		while(it.hasNext())
		{
			MD_Field f = it.next();
			if(f.getLevel() == level) it.remove();
		}	
	}	


	

}
