package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Scroller;

import com.mycompany.CMSBHelpdesk.helpers.DBController;


public class TextEditor extends ActionBarActivity {

    EditText mTextEdit;
    Button mDone;
    // DB Class to perform DB related operations
    DBController control = new DBController(this);
    int requestCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_editor);

        //set actionbar colour
        android.support.v7.app.ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(MainActivity.colorAB)));

        //Default Back Button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String type = getIntent().getStringExtra("mType");
        switch(type){
            case "desc":
                setTitle("Case Description");
                break;
            case "actionT":
                setTitle("Action Taken");
                break;
        }
        mTextEdit = (EditText) findViewById(R.id.textEdit);
        mDone = (Button) findViewById(R.id.done);
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnText();
            }
        });
        getText();
    }

    public void getText(){
        String textValue = getIntent().getStringExtra("text");
        mTextEdit.setText(textValue);
    }

    public void returnText(){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", mTextEdit.getText().toString());
        setResult(Activity.RESULT_OK, returnIntent);
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_text_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Context context = this;

        //noinspection SimplifiableIfStatement
        if(id==R.id.done){
            returnText();
            return true;
        }

        if (id == R.id.action_settings) {
            Intent intent = new Intent(context, Settings.class);
            startActivity(intent);
            return true;
        }
        if (id == android.R.id.home) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
            this.finish();
            return true;
        }
        if(id==R.id.mainMenu){
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        /**if(id == R.id.log_out) {
            control.refreshCases("cases");
            sharedPreference.delete(this);
            Intent intent = new Intent(context, Settings.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }
}
