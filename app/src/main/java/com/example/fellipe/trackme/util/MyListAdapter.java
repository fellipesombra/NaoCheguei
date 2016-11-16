package com.example.fellipe.trackme.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.ContactActivity;
import com.example.fellipe.trackme.MainActivity;
import com.example.fellipe.trackme.R;
import com.example.fellipe.trackme.dto.ContatoDTO;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Fellipe on 16/11/2016.
 */
public class MyListAdapter extends ArrayAdapter<ContatoDTO> {

    private final Context context;
    private final ArrayList<ContatoDTO> contactList;

    public MyListAdapter(Context context, ArrayList<ContatoDTO> contactList) {
        super(context, R.layout.simple_row, contactList);
        this.context = context;
        this.contactList = contactList;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.simple_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.rowTextView);
        textView.setText(contactList.get(position).getEmail());
        ImageButton deleteButton = (ImageButton) rowView.findViewById(R.id.btn_delete);

        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                deleteContact(contactList.get(position).getId(), position);
            }
        });

        return rowView;
    }

    public void deleteContact(String id,final int position) {
        String extra = "/delete/"+ id;
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, context.getString(R.string.contact_rest_url)+extra, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_DELETED.getStatusCode()){
                                contactList.remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Contato removido", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(context, "Erro no servidor. Tente novamente mais tarde.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(context, "Erro no servidor. Tente novamente mais tarde.", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, "Erro no servidor. Tente novamente mais tarde.", Toast.LENGTH_SHORT).show();
                    }
                }){
        };
        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
