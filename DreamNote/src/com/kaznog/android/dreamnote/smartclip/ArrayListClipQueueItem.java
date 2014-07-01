package com.kaznog.android.dreamnote.smartclip;

import java.util.ArrayList;

public class ArrayListClipQueueItem extends ArrayList<ClipQueueItem> {

	/**
	 *
	 */
	private static final long serialVersionUID = -627975335513295159L;
	@Override
	public boolean add(ClipQueueItem object) {
		int index = this.size();
		object.setQueueindex(index);
//		Date now = new Date();
//		object.setNotifyID(now.hashCode() & 0x7fffffff);
		return super.add(object);
	}
	@Override
	public void add(int index, ClipQueueItem object) {
		object.setQueueindex(index);
		super.add(index, object);
	}
/*
	public int getQueueCount() {
		int result = 0;
		if(!this.isEmpty()) {
			for(int i = 0; i < this.size(); i++) {
				if(this.get(i).getProgress() != 100) {
					result++;
				}
			}
		}
		return result;
	}
*/
	public ClipQueueItem getNextQueueItem() {
		ClipQueueItem cq = null;
		for(int i = 0; i < this.size(); i++) {
			int progress = this.get(i).getProgress();
			if(progress != 100 && progress != -1 && progress != -2) {
				cq = this.get(i);
				break;
			}
		}
		return cq;
	}
}
