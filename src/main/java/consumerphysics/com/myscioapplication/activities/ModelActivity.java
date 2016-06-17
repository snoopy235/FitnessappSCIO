package consumerphysics.com.myscioapplication.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.consumerphysics.android.sdk.callback.cloud.ScioCloudModelsCallback;
import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.sciosdk.ScioCloud;

import java.util.ArrayList;
import java.util.List;

import consumerphysics.com.myscioapplication.R;


public class ModelActivity extends Activity {

    private final static String TAG = ModelActivity.class.getSimpleName();

    private ListView lv;

    public class ModelAdapter extends ArrayAdapter<ScioModel> {
        public ModelAdapter(Context context, List<ScioModel> devices) {
            super(context, 0, devices);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            ScioModel model = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.simple_item, parent, false);
            }

            // Lookup view for data population
            TextView tvName = (TextView) convertView.findViewById(R.id.tvName);

            // Populate the data into the template view using the data object
            tvName.setText(model.getCollectionName() + " - " + model.getName());

            // Return the completed view to render on screen
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);

        setTitle("Select Model");

        List<ScioModel> modelArrayList = new ArrayList<>();
        final ModelAdapter adp = new ModelAdapter(this, modelArrayList);

        lv = (ListView) findViewById(R.id.listView);
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        lv.setMultiChoiceModeListener(new ModeCallback());

        lv.setAdapter(adp);

        ScioCloud cloud = new ScioCloud(this);

        if (cloud == null || !cloud.hasAccessToken()) {
            Log.d(TAG, "Can not retrieve model. User is not logged in");
            Toast.makeText(getApplicationContext(), "Can not retrieve model. User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScioModel model = adp.getItem(position);
                storeSelectedModel(model);
                Toast.makeText(getApplicationContext(), model.getName() + " was selected", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        cloud.getModels(new ScioCloudModelsCallback() {
            @Override
            public void onSuccess(List<ScioModel> models) {
                adp.addAll(models);
            }

            @Override
            public void onError(int code, String msg) {
                Log.e(TAG, "Error " + code + " while retrieving models :" + msg);
                Toast.makeText(getApplicationContext(), "Error while retrieving models", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void storeSelectedModel(final ScioModel model) {
        SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        edit.putString("model_id", model.getId());
        edit.putString("model_name", model.getCollectionName() + " - " + model.getName());

        edit.commit();
    }

    private void storeSelectedModels(final List<ScioModel> models) {
        SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();

        String modelIds = "";
        String modelNames = "";
        for(ScioModel scioModel : models) {
            modelNames += scioModel.getName();
            modelNames += ",";
            modelIds += scioModel.getId();
            modelIds += ",";
        }

        modelIds = modelIds.substring(0, modelIds.length() - 1);
        modelNames = modelNames.substring(0, modelNames.length() - 1);

        edit.putString("model_id", modelIds);
        edit.putString("model_name", modelNames);

        edit.commit();
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.model_selector_menu, menu);
            mode.setTitle("Select Items");
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.done:
                    if (lv.getCheckedItemCount() == 0) {
                        return true;
                    }

                    List<ScioModel> scioModels = new ArrayList<>();
                    for (int i = 0; i < lv.getChildCount(); i++) {
                        if (lv.getCheckedItemPositions().get(i) == true) {
                            scioModels.add((ScioModel) lv.getAdapter().getItem(i));
                        }
                    }

                    storeSelectedModels(scioModels);
                    Toast.makeText(getApplicationContext(), scioModels.size() + " selected", Toast.LENGTH_SHORT).show();

                    finish();
                    break;
            }

            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            for (int i = 0; i < lv.getChildCount(); i++) {
                lv.getChildAt(i).setBackgroundColor(getResources().getColor(R.color.transparent));
            }
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            View view = lv.getChildAt(position);
            if (checked) {
                view.setBackgroundColor(getResources().getColor(R.color.grey));
            }
            else {
                view.setBackgroundColor(getResources().getColor(R.color.transparent));
            }

            final int checkedCount = lv.getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setSubtitle(null);
                    break;
                case 1:
                    mode.setSubtitle("One item selected");
                    break;
                default:
                    mode.setSubtitle("" + checkedCount + " items selected");
                    break;
            }
        }
    }
}
