package br.com.fellipe.naocheguei.util;


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
import br.com.fellipe.naocheguei.R;
import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.enums.RestResponseStatus;
import br.com.fellipe.naocheguei.service.ContactService;
import br.com.fellipe.naocheguei.util.rest.CustomRequest;
import br.com.fellipe.naocheguei.util.rest.MySingleton;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.fellipe.naocheguei.dto.ContatoDTO;
import br.com.fellipe.naocheguei.util.rest.MySingleton;


/**
 * Created by Fellipe on 16/11/2016.
 */
public class MyListAdapter extends ArrayAdapter<ContatoDTO> {

    private final Context context;
    private ContactService contactService;

    public MyListAdapter(Context context) {
        super(context, br.com.fellipe.naocheguei.R.layout.simple_row, Session.getInstance().getContacts());
        contactService = new ContactService(context);
        this.context = context;
    }


    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(br.com.fellipe.naocheguei.R.layout.simple_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(br.com.fellipe.naocheguei.R.id.rowTextView);
        textView.setText(Session.getInstance().getContacts().get(position).getEmail());
        ImageButton deleteButton = (ImageButton) rowView.findViewById(br.com.fellipe.naocheguei.R.id.btn_delete);

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
        builder.setTitle(context.getString(br.com.fellipe.naocheguei.R.string.dialog_msg_del_contact)+"?")
                .setMessage(Session.getInstance().getContacts().get(position).getEmail())
                .setPositiveButton(br.com.fellipe.naocheguei.R.string.erase,new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteContact(Session.getInstance().getContacts().get(position).getId(), position);
                    }
                })
                .setNegativeButton(br.com.fellipe.naocheguei.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        return builder.create();
    }

    public void deleteContact(String id,final int position) {
        String extra = "/delete/"+ id;
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, context.getString(br.com.fellipe.naocheguei.R.string.contact_rest_url)+extra, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            if(!response.isNull("status") && response.getInt("status") == RestResponseStatus.CONTACT_DELETED.getStatusCode()){
                                Session.getInstance().getContacts().remove(position);
                                notifyDataSetChanged();
                                Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_contact_deleted, Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error , Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, br.com.fellipe.naocheguei.R.string.msg_server_error, Toast.LENGTH_SHORT).show();
                    }
                }){
        };
        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
