package consumerphysics.com.myscioapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.consumerphysics.android.sdk.model.ScioModel;
import com.consumerphysics.android.sdk.model.attribute.ScioDatetimeAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioNumericAttribute;
import com.consumerphysics.android.sdk.model.attribute.ScioStringAttribute;

import java.util.List;

import consumerphysics.com.myscioapplication.R;

/**
 * Created by nadavg on 16/02/2016.
 */
public class ScioModelAdapter extends ArrayAdapter<ScioModel> {
    public ScioModelAdapter(final Context context, final List<ScioModel> devices) {
        super(context, 0, devices);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.results_item, parent, false);
        }

        final TextView attributeName = (TextView) convertView.findViewById(R.id.attribute_name);
        final TextView attributeValue = (TextView) convertView.findViewById(R.id.attribute_value);

        final ScioModel model = getItem(position);

        final String value;
        String unit = null;

        /**
         * Classification model will return a STRING value.
         * Estimation will return the NUMERIC value.
         */
        switch (model.getAttributes().get(0).getAttributeType()) {
            case STRING:
                value = ((ScioStringAttribute) (model.getAttributes().get(0))).getValue();
                break;
            case NUMERIC:
                value = String.valueOf(((ScioNumericAttribute) (model.getAttributes().get(0))).getValue());
                unit = model.getAttributes().get(0).getUnits();
                break;
            case DATE_TIME:
                value = ((ScioDatetimeAttribute) (model.getAttributes().get(0))).getValue().toString();
                break;
            default:
                value = "Unknown";
        }

        attributeName.setText(model.getName());

        if (model.getType().equals(ScioModel.Type.ESTIMATION)) {
            attributeValue.setText(value + unit);
        }
        else {
            attributeValue.setText(value + " (" + String.format("%.2f", model.getConfidence()) + ")");
        }

        return convertView;
    }
}
