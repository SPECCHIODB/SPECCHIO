package ch.specchio.eav_db;

import java.util.ArrayList;

import ch.specchio.types.MetaParameter;

public class FrameMetaparameterStructure {
	
	int frame_id;
	ArrayList<MetaParameter> mps = new ArrayList<MetaParameter>();	
	
	public FrameMetaparameterStructure(int curr_frame_id) {
		this.frame_id = curr_frame_id;
	}

	
	
	public void add(MetaParameter mp) {
		mps.add(mp);
		
	}
	
	public void addAll(ArrayList<MetaParameter> mps) {
		this.mps.addAll(mps);
		
	}	
}
