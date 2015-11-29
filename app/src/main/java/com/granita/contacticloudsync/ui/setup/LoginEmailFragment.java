/*
 * Copyright © 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package com.granita.contacticloudsync.ui.setup;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.net.URI;
import java.net.URISyntaxException;

import com.granita.contacticloudsync.Constants;
import com.granita.contacticloudsync.R;
import com.granita.contacticloudsync.resource.ServerInfo;

public class LoginEmailFragment extends Fragment implements TextWatcher {

    protected EditText editEmail, editPassword;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.setup_login_email, container, false);

        editEmail = (EditText)v.findViewById(R.id.email_address);
        editEmail.addTextChangedListener(this);

        editPassword = (EditText)v.findViewById(R.id.password);
        editPassword.addTextChangedListener(this);

        setHasOptionsMenu(true);
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.only_next, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next:
                try {
                    String email = editEmail.getText().toString();
                    Bundle args = new Bundle();
                    args.putSerializable(QueryServerDialogFragment.KEY_SERVER_INFO, new ServerInfo(
                            //custom start
							//new URI("mailto:" + email),
							new URI("mailto:" + "test@icloud.com"),
							//custom end
                            email,
                            editPassword.getText().toString(),
                            //custom start
                            //true
                            false
                            //custom end
                    ));

                    DialogFragment dialog = new QueryServerDialogFragment();
                    dialog.setArguments(args);
                    dialog.show(getFragmentManager(), QueryServerDialogFragment.class.getName());
                } catch (URISyntaxException e) {
                    Constants.log.debug("Invalid email address", e);
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        // set focus and show soft keyboard
        if (editEmail.requestFocus()) {
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editEmail, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    // input validation

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean emailOk = false,
                passwordOk = editPassword.getText().length() > 0;

        String email = editEmail.getText().toString();
        try {
			//custom start
            //URI uri = new URI("mailto:" + email);
			URI uri = new URI("mailto:" + "test@icloud.com");
			//custom end
            if (uri.isOpaque()) {
                int pos = email.lastIndexOf("@");
                if (pos != -1)
                    emailOk = !email.substring(pos + 1).isEmpty();
            }
        } catch (URISyntaxException e) {
            // invalid mailto: URI
        }

        MenuItem item = menu.findItem(R.id.next);
        item.setEnabled(emailOk && passwordOk);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

}
