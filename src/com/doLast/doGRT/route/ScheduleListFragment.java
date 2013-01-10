package com.doLast.doGRT.route;

import java.util.Calendar;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MergeCursor;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.doLast.doGRT.R;
import com.doLast.doGRT.database.DatabaseSchema;
import com.doLast.doGRT.database.DatabaseSchema.CalendarColumns;
import com.doLast.doGRT.database.DatabaseSchema.RoutesColumns;
import com.doLast.doGRT.database.DatabaseSchema.StopTimesColumns;
import com.doLast.doGRT.database.DatabaseSchema.TripsColumns;

public class ScheduleListFragment extends SherlockListFragment {
	public static final String mTag = "ScheduleListFragment";
	private RoutesActivity mActivity = null;
	
	// Cursor adapter
	private SimpleCursorAdapter adapter = null;
	
	// Stop id, name and title
	private String stop_id = null;
	private String stop_name = null;
	private String stop_title = null;
	
	// Route id for individual route
	private String route_id = null;
	
	// Today's service id
	private String service_ids = null;	
	private String yesterday_service_ids = null;
	
	// Left buses display offset
	private final int LEFT_BUSES_OFFSET = 2;	
	
	// Display type. Mixed schedule as default
	private int display_type = RoutesActivity.SCHEDULE_MIXED;
	
	// To control back key when single route is displayed
	private boolean single_route = false;
	
	// Constants for save instance
	private final String LAST_POS = "last_pos"; // The position where user last viewed
	private final String LAST_VIEW = "last_view"; // Whether user has selected a single route
	private final String SAVE_ROUTE_ID = "save_route_id";
	
	// Alias of days with custom days
	static public final int DAYS[] = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
									Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
									Calendar.SATURDAY};
	
	static ScheduleListFragment newInstance() {
    	ScheduleListFragment f = new ScheduleListFragment();
        return f;
    }

	public void setType(int type) {
		display_type = type;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {	
	   	super.onActivityCreated(savedInstanceState);
		mActivity = (RoutesActivity)getActivity();
		
		// Get the stop id, name and title
		stop_id = mActivity.getStopId();
		stop_name = mActivity.getStopName();
		stop_title = mActivity.getStopTitle();
		
        // Determine service id
    	service_ids = getTodayServiceId();
    	yesterday_service_ids = getYesterdayServiceId();		
    	
    	//displaySchedule();
    	
    	ListView list_view = getListView();
    	TextView text_view = (TextView)mActivity.findViewById(R.id.schedule_empty_view);
    	list_view.setEmptyView(text_view);    
    	
    	// Restore position of list and last view
    	if (savedInstanceState != null) {
    		//setSelection(savedInstanceState.getInt(LAST_POS, 0));
    		single_route = savedInstanceState.getBoolean(LAST_VIEW, false);
    		route_id = savedInstanceState.getString(SAVE_ROUTE_ID);
    		if (single_route) displaySchedule(stop_id, route_id);
    	}    	
    	
	}		
	
    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		// Save instances
		//outState.putInt(LAST_POS, getListView().getSelectedItemPosition());
		outState.putBoolean(LAST_VIEW, single_route);
		outState.putString(SAVE_ROUTE_ID, route_id);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
    	if (!isSingleRouteDisplayed() && display_type == RoutesActivity.SCHEDULE_SELECT) {
    		TwoLineListItem text_view = (TwoLineListItem)v;
    		route_id = (String) text_view.getText2().getText();
    		displaySchedule(stop_id, route_id);
    	}
    	
		super.onListItemClick(l, v, position, id);
	}

	public void backKeyPressed() {
    	single_route = false;
    	displayRoutes(stop_id);    	
    }
	
	public void displaySchedule() {
    	// Display schedule
    	switch(display_type) {
    	case RoutesActivity.SCHEDULE_MIXED:
			displaySchedule(stop_id, null);
			break;
    	case RoutesActivity.SCHEDULE_SELECT:
			displayRoutes(stop_id);
		    single_route = false;
			break;
    	default:
		}
	}
    
    /** 
     * Retrieve service ids
     * @param today day of week in int (Java Calendar)
     */
	private String getServiceId(int day) {
    	String service_ids = new String("");
    	String selection = new String("");

        switch(day) {
        case Calendar.SUNDAY:
        	selection = "sunday";
        	break;
        case Calendar.THURSDAY:
        	selection = "thursday";
        	break;
        case Calendar.MONDAY:
        	selection = "monday";
        	break;
        case Calendar.TUESDAY:
        	selection = "tuesday";
        	break;
        case Calendar.WEDNESDAY:
        	selection = "wednesday";
        	break;
        case Calendar.FRIDAY:
        	selection = "friday";
        	break;
        case Calendar.SATURDAY:
        	selection = "saturday";
        	break;
        default:
        	break;
        }

        String[] projection = { CalendarColumns.SERVICE_ID };
        Cursor services = mActivity.managedQuery(CalendarColumns.CONTENT_URI, projection, selection, null, null);
        services.moveToFirst();
        for(int i = 0; i < services.getCount() - 1; i += 1){
        	service_ids += " OR " + TripsColumns.SERVICE_ID + " = '" + services.getString(0) + "'";
        	services.moveToNext();
        }
        service_ids = TripsColumns.SERVICE_ID + " = '" + services.getString(0) + "'" + service_ids;
        //services.close();
        return service_ids;
    }
	
	public String getTodayServiceId() {
    	Calendar calendar = Calendar.getInstance();
    	return getServiceId(calendar.get(Calendar.DAY_OF_WEEK));
	}
	
	public String getYesterdayServiceId() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_WEEK, -1);
		return getServiceId(calendar.get(Calendar.DAY_OF_WEEK));
	}
	
	public void setServiceId(int position) {
		// Set up service id
		service_ids = getServiceId(DAYS[position]);
		yesterday_service_ids = getServiceId(DAYS[(position + 6) % 7]);
		
	}    
	
	// Display the schedule with given stop id and route id, route_id can be null if want mixed schedule
    private void displaySchedule(String stop_id, String route_id) {   
        // Remember to perform an alias of our own primary key to _id so the adapter knows what to do
        String[] projection = { StopTimesColumns.TABLE_NAME + "." + StopTimesColumns.TRIP_ID + " as _id", 
        						StopTimesColumns.DEPART,
        						RoutesColumns.LONG_NAME,
        						RoutesColumns.TABLE_NAME + "." + RoutesColumns.ROUTE_ID,
        						TripsColumns.TABLE_NAME + "." + TripsColumns.HEADSIGN };
        // Some complex selection for selecting from 3 tables
        String stop_time_id = StopTimesColumns.TABLE_NAME + "." + StopTimesColumns.STOP_ID;
        String stop_time_trip_id = StopTimesColumns.TABLE_NAME + "." + StopTimesColumns.TRIP_ID;
        String trip_trip_id = TripsColumns.TABLE_NAME + "." + TripsColumns.TRIP_ID;
        String trip_route_id = TripsColumns.TABLE_NAME + "." + TripsColumns.ROUTE_ID;
        String trip_service_id = TripsColumns.TABLE_NAME + "." + TripsColumns.SERVICE_ID;
        String route_route_id = RoutesColumns.TABLE_NAME + "." + RoutesColumns.ROUTE_ID;
        
        String selection =  stop_time_id + " = " + stop_id + " AND " +
        					stop_time_trip_id + " = " + trip_trip_id + " AND " +
        					trip_route_id + " = " + route_route_id + " AND " +
        					"(" + service_ids + ")";
        selection = checkSingleRoutSelection(selection, route_id);
        String orderBy = StopTimesColumns.DEPART;
        // Today's schedule
        Cursor stop_times = mActivity.managedQuery(
        		DatabaseSchema.STTRJ_CONTENT_URI, projection, selection, null, orderBy);      
        
        // Yesterday's schedule
        // Modify selection
        selection =  stop_time_id + " = " + stop_id + " AND " +
				stop_time_trip_id + " = " + trip_trip_id + " AND " +
				trip_route_id + " = " + route_route_id + " AND " +
				"(" + yesterday_service_ids + ")" + " AND " +
				StopTimesColumns.DEPART + " >= " + 240000;
        selection = checkSingleRoutSelection(selection, route_id);      
        Cursor yesterday_stop_times = mActivity.managedQuery(
        		DatabaseSchema.STTRJ_CONTENT_URI, projection, selection, null, orderBy);
        
        MergeCursor merge_stop_times = new MergeCursor(new Cursor[] { yesterday_stop_times, stop_times });
        
        String[] uiBindFrom = { StopTimesColumns.DEPART, RoutesColumns.ROUTE_ID, RoutesColumns.LONG_NAME };
        int[] uiBindTo = { R.id.depart_time, R.id.route_name };                 
                
        // Move adapter to schedule close to current time
        Calendar time = Calendar.getInstance();
        int cur_time = time.get(Calendar.HOUR_OF_DAY) * 10000;
        cur_time += time.get(Calendar.MINUTE) * 100;
        cur_time += time.get(Calendar.SECOND);
        
        // Iterate through cursor
        int cur_pos = 0;
        stop_times.moveToFirst();
        int time_count = stop_times.getCount();
        for(int i = 0; i < time_count; i += 1) {        	
        	int depart = stop_times.getInt(1); // Get the departure time from cursor as and integer
        	if ( depart > cur_time ) {
        		cur_pos = i;
        		//section = COMING_BUSES;
        		break;
        	}
        	stop_times.moveToNext();
        }            
        
        // Includes yesterday's time into the offset to make the divider accurate
        cur_pos += yesterday_stop_times.getCount();
        
        // Assign adapter to ListView
        adapter = new ScheduleAdapter(mActivity, R.layout.schedule, merge_stop_times,
                uiBindFrom, uiBindTo, cur_pos);
        setListAdapter(adapter);
        if (cur_pos >= LEFT_BUSES_OFFSET && time_count - cur_pos >= LEFT_BUSES_OFFSET) cur_pos -= LEFT_BUSES_OFFSET;
        setSelection(cur_pos);         
    }
    
    private void displayRoutes(String stop_id) {    	
        // Remember to perform an alias of our own primary key to _id so the adapter knows what to do
        String[] projection = { RoutesColumns.TABLE_NAME + "." + RoutesColumns.ROUTE_ID + " as _id", 
        						RoutesColumns.LONG_NAME };
        // Some complex selection for selecting from 3 tables
        String stop_time_id = StopTimesColumns.TABLE_NAME + "." + StopTimesColumns.STOP_ID;
        String stop_time_trip_id = StopTimesColumns.TABLE_NAME + "." + StopTimesColumns.TRIP_ID;
        String trip_trip_id = TripsColumns.TABLE_NAME + "." + TripsColumns.TRIP_ID;
        String trip_route_id = TripsColumns.TABLE_NAME + "." + TripsColumns.ROUTE_ID;
        String trip_service_id = TripsColumns.TABLE_NAME + "." + TripsColumns.SERVICE_ID;
        String route_route_id = RoutesColumns.TABLE_NAME + "." + RoutesColumns.ROUTE_ID;
        
        String selection =  stop_time_id + " = " + stop_id + " AND " +
        					stop_time_trip_id + " = " + trip_trip_id + " AND " +
        					trip_route_id + " = " + route_route_id + " AND " +
        					"(" + service_ids + ")";
        Cursor routes = mActivity.managedQuery(
        		DatabaseSchema.STTRJ_CONTENT_URI, projection, selection, null, null);
        
        String[] uiBindFrom = { RoutesColumns.LONG_NAME, "_id" };
        int[] uiBindTo = { android.R.id.text1, android.R.id.text2 }; 
        
        // Assign adapter to list view
        adapter = new SimpleCursorAdapter(mActivity, android.R.layout.simple_list_item_2, routes,
        		uiBindFrom, uiBindTo);
        setListAdapter(adapter);
    }    
    
    // Check the selection statement for the query, return the updated selection
    private String checkSingleRoutSelection(String selection, String route_id) {
    	String ret_selection = selection;
        if (route_id != null) {
        	ret_selection += " AND " + RoutesColumns.TABLE_NAME + "." + RoutesColumns.ROUTE_ID + " = " + route_id;
        	single_route = true;
        }
        return ret_selection;
    }
    
    public boolean isSingleRouteDisplayed() {
    	return single_route;
    }        
}
