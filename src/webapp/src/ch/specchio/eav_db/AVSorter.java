package ch.specchio.eav_db;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import ch.specchio.factories.SpectralBrowserFactory;
import ch.specchio.types.AVMatchingList;
import ch.specchio.types.AVMatchingListCollection;
import ch.specchio.types.MetaParameter;
import ch.specchio.types.hierarchy_node;
import ch.specchio.types.spectral_node_object;


public class AVSorter {
	
	EAVDBServices eav;
	ArrayList<AVMatchingList> spectrum_id_lists = new ArrayList<AVMatchingList>();
	
	public AVSorter(EAVDBServices eav){
		this.eav = eav;
	}
	
	public void insert_into_lists(AVMatchingList av_input) 
	{
		// get list of attribute ids
		int attr_ids[] = new int[av_input.getAttributeNames().size()];
		
		for(int i=0;i<av_input.getAttributeNames().size();i++){			
			attr_ids[i] = eav.ATR.get_attribute_id(av_input.getAttributeNames().get(i));			
		}
		
		
		// load all metaparameters of all storage levels
		ArrayList<FrameMetaparameterStructure> fms_list = eav.load_metaparameters(MetaParameter.SPECTRUM_LEVEL, av_input.getSpectrumIds(), attr_ids);
		ArrayList<Integer> hierarchy_ids = eav.getHierarchyIds(av_input.getSpectrumIds());
		ArrayList<FrameMetaparameterStructure> fms_list_hierarchy_level = eav.load_metaparameters(MetaParameter.HIERARCHY_LEVEL, hierarchy_ids, attr_ids);
		
		ListIterator<FrameMetaparameterStructure> li = fms_list_hierarchy_level.listIterator();			

		// get fms on spectrum level, but spectra must be within original list as well!
		SpectralBrowserFactory factory = new SpectralBrowserFactory(eav.specchioFactory);
		while(li.hasNext())
		{
			FrameMetaparameterStructure fms = li.next();		
			spectral_node_object node = new hierarchy_node(fms.frame_id, "", "");
			List<Integer> ids = factory.getDescendentSpectrumIds(node);
			
			ListIterator<Integer> li_spec = ids.listIterator();
			while(li_spec.hasNext())
			{
				Integer current_spec_id = li_spec.next();
				
				if(av_input.getSpectrumIds().contains(current_spec_id))
				{
					FrameMetaparameterStructure fms_new = new FrameMetaparameterStructure(current_spec_id);
					fms_new.addAll(fms.mps);
					fms_list.add(fms_new);
				}
			}
		}
		factory.dispose();
		
				
		li = fms_list.listIterator();			

		while(li.hasNext())
		{			
			FrameMetaparameterStructure fms = li.next();			
			insert_into_lists(fms.frame_id, fms.mps);	
		}
		
		
		

	}
	
	
	private void insert_into_lists(int spectrum_id, ArrayList<MetaParameter> mps) {
		boolean done = false;
		
		
		// try all existing lists
		ListIterator<AVMatchingList> id_li = spectrum_id_lists.listIterator();
		
		while(id_li.hasNext() && !done)
		{
			AVMatchingList id_list = id_li.next();			
			done = add_via_metaparameter_if_matching(id_list, spectrum_id, mps);
		}
		
		
		if (done == false)
		{
			// insert a new list and add this frame			
			AVMatchingList id_list = new AVMatchingList();
			add_via_metaparameter_if_matching(id_list, spectrum_id, mps);
			spectrum_id_lists.add(id_list);		
		}
		
	}
	
	
	
	public AVMatchingListCollection getAVMatchingListCollection() {
		
		AVMatchingListCollection collection = new AVMatchingListCollection();
		
		collection.setSpectrum_id_lists(spectrum_id_lists);

		return collection;
	}

	private boolean add_via_metaparameter_if_matching(AVMatchingList id_list, int spectrum_id, ArrayList<MetaParameter> mps) {
	
		
		boolean matches = true;

		ListIterator<MetaParameter> li = mps.listIterator();
		
		if (id_list.getMetaparameters().size() == 0)
		{
			// this is a first matching call, therefore we accept the match and add the according attributes and values	
			matches = true;

			
			while(li.hasNext())
			{			
				MetaParameter mp = li.next();				
				id_list.getMetaparameters().add(mp);				
			}			
			
		} else
		{
			// find out if the properties match
			
			while(li.hasNext() && matches)
			{			
				MetaParameter mp = li.next();
				
				// try to find a match of this mp in the properties list
				boolean sub_match = false;
				ListIterator<MetaParameter> prop_li = id_list.getMetaparameters().listIterator();
				
				while(prop_li.hasNext())
				{
					MetaParameter curr_prop = prop_li.next();
					
					boolean tmp_match = mp.getAttributeId().equals(curr_prop.getAttributeId()) && mp.getValue().equals(curr_prop.getValue()) ;
					
					sub_match |= tmp_match;
				}
				
				matches &= sub_match;				
			
			}	
		}
		
		
		if (matches)
		{	
			id_list.getSpectrumIds().add(spectrum_id);
		}

		
		return matches;		
		
		
		
		
	
	}
	

	
	
//	public void insert_into_lists(ArrayList<Integer>  frame_ids, ArrayList<Object> values) 
//	{
//		
//		ListIterator<Integer> frame_li = frame_ids.listIterator();
//		ListIterator<Object> values_li = values.listIterator();
//		
//		
//		while(frame_li.hasNext())
//		{
//			boolean done = false;
//			Integer frame_id = frame_li.next();
//			Object value = values_li.next();
//			
//			// try all existing lists
//			ListIterator<AV_MatchingList> id_li = frame_id_lists.listIterator();
//			
//			while(id_li.hasNext() && !done)
//			{
//				AV_MatchingList id_list = id_li.next();			
//				done = id_list.add_if_matching(frame_id, value);
//			}
//			
//			
//			if (done == false)
//			{
//				// insert a new list and add this frame			
//				AV_MatchingList id_list = new AV_MatchingList();
//				id_list.add_if_matching(frame_id, value);
//				frame_id_lists.add(id_list);		
//			}						
//			
//		}
//		
//	}
		
	

}
