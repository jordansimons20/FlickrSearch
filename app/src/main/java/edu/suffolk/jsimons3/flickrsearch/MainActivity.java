package edu.suffolk.jsimons3.flickrsearch;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;

public class MainActivity extends ListActivity {

    // name of SharedPreferences XML file that stores the saved searches
    private static final String SEARCHES = "searches";

    private EditText queryEditText; // EditText where user enters a query
    private EditText tagEditText; // EditText where user tags a query
    private SharedPreferences savedSearches; // user's favorite searches
    private ArrayList<String> tags; // list of tags for saved searches
    private ArrayAdapter<String> adapter; // binds tags to ListView
    private String COLOR_NUM = "";


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // get references to the EditTexts
        queryEditText = (EditText) findViewById(R.id.queryeditText);
        tagEditText = (EditText) findViewById(R.id.tageditText);

        // get the SharedPreferences containing the user's saved searches
        savedSearches = getSharedPreferences(SEARCHES, MODE_PRIVATE);

        // store the saved tags in an ArrayList then sort them
        tags = new ArrayList<String>(savedSearches.getAll().keySet());
        Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);

        // create ArrayAdapter and use it to bind tags to the ListView
        adapter = new ArrayAdapter<String>(this, R.layout.list_items, tags);
        setListAdapter(adapter);

        // register listener to save a new or edited search
        ImageButton saveButton =
                (ImageButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(saveButtonListener);

        // register listener that searches Twitter when user touches a tag
        getListView().setOnItemClickListener(itemClickListener);

        // set listener that allows user to delete or edit a search
        getListView().setOnItemLongClickListener(itemLongClickListener);



    }

    // saveButtonListener saves a tag-query pair into SharedPreferences
    public View.OnClickListener saveButtonListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            // create tag if neither queryEditText nor tagEditText is empty
            if (queryEditText.getText().length() > 0 &&
                    tagEditText.getText().length() > 0)
            {
                addTaggedSearch(queryEditText.getText().toString(),
                        tagEditText.getText().toString());
                queryEditText.setText(""); // clear queryEditText
                tagEditText.setText(""); // clear tagEditText

                ((InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                        tagEditText.getWindowToken(), 0);  //Hide keyboard
            }
            else // display message asking user to provide a query and a tag
            {
                // create a new AlertDialog Builder
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(MainActivity.this);

                // set dialog's message to display
                builder.setMessage(R.string.missingMessage);

                // provide an OK button that simply dismisses the dialog
                builder.setPositiveButton(R.string.OK, null);

                // create AlertDialog from the AlertDialog.Builder
                AlertDialog errorDialog = builder.create();
                errorDialog.show(); // display the modal dialog
            }
        } // end method onClick
    }; // end OnClickListener anonymous inner class

    // itemClickListener launches a web browser to display search results
    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view,
                                int position, long id)
        {
            // get query string and create a URL representing the search
            String tag = ((TextView) view).getText().toString();
            String urlString = getString(R.string.searchURL) +
                    Uri.encode(savedSearches.getString(tag, ""), "UTF-8") + COLOR_NUM;

            // create an Intent to launch a web browser
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(urlString));

            startActivity(webIntent); // launches web browser to view results
        }
    }; // end itemClickListener declaration

    // add new search to the save file, then refresh all Buttons
    private void addTaggedSearch(String query, String tag)
    {
        // get a SharedPreferences.Editor to store new tag/query pair
        SharedPreferences.Editor preferencesEditor = savedSearches.edit();
        preferencesEditor.putString(tag, query); // store current search
        preferencesEditor.apply(); // store the updated preferences

        // if tag is new, add to and sort tags, then display updated list
        if (!tags.contains(tag))
        {
            tags.add(tag); // add new tag
            Collections.sort(tags, String.CASE_INSENSITIVE_ORDER);
            adapter.notifyDataSetChanged(); // rebind tags to ListView
        }
    }

    // itemLongClickListener displays a dialog allowing the user to delete
    // or edit a saved search
    AdapterView.OnItemLongClickListener itemLongClickListener =
            new AdapterView.OnItemLongClickListener()
            {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view,
                                               int position, long id)
                {
                    // get the tag that the user long touched
                    final String tag = ((TextView) view).getText().toString();

                    // create a new AlertDialog
                    AlertDialog.Builder builder =
                            new AlertDialog.Builder(MainActivity.this);

                    // set the AlertDialog's title
                    builder.setTitle(
                            getString(R.string.shareEditDeleteTitle, tag));

                    // set list of items to display in dialog
                    builder.setItems(R.array.dialog_items,
                            new DialogInterface.OnClickListener()
                            {
                                // responds to user touch by sharing, editing or
                                // deleting a saved search
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    switch (which)
                                    {
                                        case 0: // share
                                            shareSearch(tag);
                                            break;
                                        case 1: // edit
                                            // set EditTexts to match chosen tag and query
                                            tagEditText.setText(tag);
                                            queryEditText.setText(
                                                    savedSearches.getString(tag, ""));
                                            break;
                                        case 2: // delete
                                            deleteSearch(tag);
                                            break;
                                        case 3: //set color
                                            setColor();
                                            break;
                                    }
                                }
                            } // end DialogInterface.OnClickListener
                    ); // end call to builder.setItems

                    // set the AlertDialog's negative Button
                    builder.setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener()
                            {
                                // called when the "Cancel" Button is clicked
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    dialog.cancel(); // dismiss the AlertDialog
                                }
                            }
                    ); // end call to setNegativeButton

                    builder.create().show(); // display the AlertDialog
                    return true;
                } // end method onItemLongClick
            }; // end OnItemLongClickListener declaration

    private void setColor()
    {
        CharSequence colors[] = new CharSequence[] {"None","Red", "Dark Orange", "Orange", "Pale Pink", "Lemon Yellow",
                "School Bus Yellow", "Green", "Dark Lime Green", "Cyan", "Blue", "Violet", "Pink","White",
                "Gray", "Black"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Color Filter");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Need to set to desktop site

                if (which == 0) COLOR_NUM = ""; //No color Filter

                if (which == 1) COLOR_NUM = "&color_codes=0"; //Red

                if (which == 2) COLOR_NUM = "&color_codes=1"; //Dark Orange

                if (which == 3) COLOR_NUM = "&color_codes=2"; //Orange

                if (which == 4) COLOR_NUM = "&color_codes=b"; //Pale Pink

                if (which == 5) COLOR_NUM = "&color_codes=4"; //Lemon Yellow

                if (which == 6) COLOR_NUM = "&color_codes=3"; //School Bus Yellow

                if (which == 7) COLOR_NUM = "&color_codes=5"; //Green

                if (which == 8) COLOR_NUM = "&color_codes=6"; //Dark Lime Green

                if (which == 9) COLOR_NUM = "&color_codes=7"; //Cyan

                if (which == 10) COLOR_NUM = "&color_codes=8"; //Blue

                if (which == 11) COLOR_NUM = "&color_codes=9"; //Violet

                if (which == 12) COLOR_NUM = "&color_codes=a"; //Pink

                if (which == 13) COLOR_NUM = "&color_codes=c"; //White

                if (which == 14) COLOR_NUM = "&color_codes=d"; //Gray

                if (which == 15) COLOR_NUM = "&color_codes=e"; //Black



            }
        });
        builder.show();

    }

    // allows user to choose an app for sharing a saved search's URL
    private void shareSearch(String tag)
    {
        // create the URL representing the search
        String urlString = getString(R.string.searchURL) +
                Uri.encode(savedSearches.getString(tag, ""), "UTF-8") + COLOR_NUM;

        // create Intent to share urlString
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.shareSubject));
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.shareMessage, urlString));
        shareIntent.setType("text/plain");

        // display apps that can share text
        startActivity(Intent.createChooser(shareIntent,
                getString(R.string.shareSearch)));
    }

    // deletes a search after the user confirms the delete operation
    private void deleteSearch(final String tag)
    {
        // create a new AlertDialog
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);

        // set the AlertDialog's message
        confirmBuilder.setMessage(
                getString(R.string.confirmMessage, tag));

        // set the AlertDialog's negative Button
        confirmBuilder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    // called when "Cancel" Button is clicked
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel(); // dismiss dialog
                    }
                }
        ); // end call to setNegativeButton

        // set the AlertDialog's positive Button
        confirmBuilder.setPositiveButton(getString(R.string.delete),
                new DialogInterface.OnClickListener()
                {
                    // called when "Cancel" Button is clicked
                    public void onClick(DialogInterface dialog, int id)
                    {
                        tags.remove(tag); // remove tag from tags

                        // get SharedPreferences.Editor to remove saved search
                        SharedPreferences.Editor preferencesEditor =
                                savedSearches.edit();
                        preferencesEditor.remove(tag); // remove search
                        preferencesEditor.apply(); // saves the changes

                        // rebind tags ArrayList to ListView to show updated list
                        adapter.notifyDataSetChanged();
                    }
                } // end OnClickListener
        ); // end call to setPositiveButton

        confirmBuilder.create().show(); // display AlertDialog
    } // end method deleteSearch

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}


