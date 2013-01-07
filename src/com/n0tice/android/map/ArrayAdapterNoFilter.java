/**   Copyright 2012 Tyrell Mobile

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
*/

package com.n0tice.android.map;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class ArrayAdapterNoFilter extends ArrayAdapter<String> {

    public ArrayAdapterNoFilter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    private final NoFilter NO_FILTER = new NoFilter();

    @Override
    public Filter getFilter() {
        return NO_FILTER;
    }

    private class NoFilter extends Filter {
        protected FilterResults performFiltering(CharSequence prefix) {
            return new FilterResults();
        }

        protected void publishResults(CharSequence constraint, FilterResults results) {
            // Do nothing
        }
    }
}