package com.example.fellipe.trackme.util;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
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
import com.example.fellipe.trackme.R;
import com.example.fellipe.trackme.dto.ContatoDTO;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.service.ContactService;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Fellipe on 16/11/2016.
 */
public class MyListAdapter extends ArrayAdapter<ContatoDTO> {

    private final Context context;
    private ContactService contactService;

    public MyListAdapter(Context context) {
        super(context, R.layout.simple_row, Session.getInstance().getContacts());
        contactService = new ContactService(context);
        this.context = context;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.simple_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.rowTextView);
        textView.setText(Session.getInstance().getContacts().get(position).getEmail());
        ImageButton deleteButton = (ImageButton) rowView.findViewById(R.id.btn_delete);

        deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Dialog dialog = buildConfirmationDialog(position);
                dialog.show();
            }
        });

        return rowView;
    }

    private Dialog buildConfirmationDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.dialog_msg_del_contact)+"?")
                .setMessage(Session.getInstance().getContacts().get(position).getEmail())
                .setPositiveButton(R.string.erase,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteContact(Session.getInstance().getContacts().get(position).getId(), position);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        return builder.create();
    }

    public void deleteContact(String id,final int position) {
        String extra = "/delete/"+ id;
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, context.getString(R.string.contact_rest_url)+extra, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_DELETED.getStatusCode()){
                                Session.getInstance().getContacts().remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, R.string.msg_contact_deleted, Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(context, R.string.msg_server_error , Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };
        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
