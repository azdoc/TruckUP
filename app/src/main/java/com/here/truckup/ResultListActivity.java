/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.truckup;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.search.DiscoveryResult;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.Place;
import com.here.android.mpa.search.PlaceLink;
import com.here.android.mpa.search.PlaceRequest;
import com.here.android.mpa.search.ResultListener;

/*A list view to present DiscoveryResult */
public class ResultListActivity extends ListActivity {

    public static final String EXTRA_LATITUDE="customer_destination_latitude";
    public static final String EXTRA_LONGITUDE="customer_destination_longitude";
    public static final String EXTRA_DESTINATIONPLACE="customer_destination_placename";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.result_list);
        ResultListAdapter listAdapter = new ResultListAdapter(this,
                android.R.layout.simple_list_item_1, CustomerMapActivity.s_ResultList);
        setListAdapter(listAdapter);
    }
    /* Retrieve details of the place selected */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        DiscoveryResult result = CustomerMapActivity.s_ResultList.get(position);
        if (result.getResultType() == DiscoveryResult.ResultType.PLACE) {
            /* Fire the PlaceRequest */
            PlaceLink placeLink = (PlaceLink) result;
            PlaceRequest placeRequest = placeLink.getDetailsRequest();
            placeRequest.execute(m_placeResultListener);
        } else if (result.getResultType() == DiscoveryResult.ResultType.DISCOVERY) {
            /*
             * Another DiscoveryRequest object can be obtained by calling DiscoveryLink.getRequest()
             */
            Toast.makeText(this, "This is a DiscoveryLink result", Toast.LENGTH_SHORT).show();
        }
    }

    private ResultListener<Place> m_placeResultListener = new ResultListener<Place>() {
        @Override
        public void onCompleted(Place place, ErrorCode errorCode) {
            if (errorCode == ErrorCode.NONE) {
                /*
                 * No error returned,let's show the name and location of the place that just being
                 * selected.Additional place details info can be retrieved at this moment as well,
                 * please refer to the HERE Android SDK API doc for details.
                 */
                String placeName=place.getName();
                GeoCoordinate geoCoordinate = place.getLocation().getCoordinate();
                double latitude=geoCoordinate.getLatitude();
                double longitude=geoCoordinate.getLongitude();
                Intent intent=new Intent(ResultListActivity.this,CustomerMapActivity.class);
                intent.putExtra(EXTRA_LATITUDE,latitude);
                intent.putExtra(EXTRA_LONGITUDE,longitude);
                intent.putExtra(EXTRA_DESTINATIONPLACE,placeName);
                startActivity(intent);
                finish();
                return;

            } else {
                Toast.makeText(getApplicationContext(),
                        "ERROR:Place request returns error: " + errorCode, Toast.LENGTH_SHORT)
                        .show();
            }

        }
    };
}
