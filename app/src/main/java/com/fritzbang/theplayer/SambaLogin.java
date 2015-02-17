package com.fritzbang.theplayer;

/**
 * Created by mrhynard on 2/14/2015.
 */
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.net.Uri;
import android.os.Bundle;

public class SambaLogin extends Activity {
    public String share;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        share = i.getStringExtra("share");

        setContentView(R.layout.login);

        Button btn = (Button)findViewById(R.id.submit);
        btn.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                EditText domain = (EditText)findViewById(R.id.editTextDomain);
                EditText username = (EditText)findViewById(R.id.editTextUserName);
                EditText password = (EditText)findViewById(R.id.editTextPassword);

                com.fritzbang.theplayer.DownloadService.ProvideLoginCredentials(domain.toString(), username.toString(), password.toString());

                //Intent intent = new Intent(this, com.fritzbang.theplayer.SambaExplorer.class);
                Intent intent = new Intent(v.getContext(), com.fritzbang.theplayer.SambaExplorer.class);
                intent.setData(Uri.parse(share));
                startActivity(intent);
            }
        });
    }

}