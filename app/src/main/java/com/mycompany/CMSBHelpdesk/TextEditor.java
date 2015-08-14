package com.mycompany.CMSBHelpdesk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.mycompany.CMSBHelpdesk.helpers.DBController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Abel Hii 2015
 *
 * Note: This activity is quite simple as it is basically just an edit text, it passes and displays information back to
 *       Case Description and Action Taken
 *
 */

public class TextEditor extends ActionBarActivity {

    EditText mTextEdit;
    Button mDone, mCancel;
    Boolean textChanged = false;

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
        getText();
        mTextEdit.addTextChangedListener(new TextWatcher() {
            String oldText = mTextEdit.getText().toString();
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(!charSequence.toString().equals(oldText)){
                    textChanged = true;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mDone = (Button) findViewById(R.id.done);
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnText();
            }
        });
        mCancel = (Button) findViewById(R.id.cancel);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager imm = (InputMethodManager)getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
                TextEditor.this.finish();
            }
        });
    }

    public void getText(){
        String textValue = getIntent().getStringExtra("text");
        mTextEdit.setText(textValue);
    }

    public void returnText(){
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result", mTextEdit.getText().toString());
        if(textChanged == true){
            returnIntent.putExtra("textChanged", "true");
        }
        setResult(Activity.RESULT_OK, returnIntent);
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
        finish();
        textChanged = false;
    }

    @Override
    public void onBackPressed(){
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTextEdit.getWindowToken(), 0);
        this.finish();
    }

    public void setTimeStamp(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy  HH:mm    ");
        String currentTimeStamp = dateFormat.format(new Date()); // Find today's date

        int start = Math.max(mTextEdit.getSelectionStart(), 0);
        int end = Math.max(mTextEdit.getSelectionEnd(), 0);
        mTextEdit.getText().replace(
                Math.min(start,end),
                Math.max(start, end),
                "\n"+currentTimeStamp+"\n", 0,
                currentTimeStamp.length()+1);
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
        if(id==R.id.timeStamp){
            setTimeStamp();
            return true;
        }
        if(id==R.id.done){
            returnText();
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
        return super.onOptionsItemSelected(item);
    }
}
