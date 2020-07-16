package edu.miracosta.cs134.flagquiz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.miracosta.cs134.flagquiz.model.Country;
import edu.miracosta.cs134.flagquiz.model.JSONLoader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Flag Quiz";

    private static final int FLAGS_IN_QUIZ = 10;
    public static final String REGIONS = "pref_regions";
    public static final String CHOICES = "pref_numberOfChoices";

    // Keeps track of the current number of choices
    private int mChoices = 4;

    // Keeps track of the current region selected
    private String mRegion = "All";
    private Button[] mButtons = new Button[8];
    private List<Country> mAllCountriesList;  // all the countries loaded from JSON
    private List<Country> mQuizCountriesList; // countries in current quiz (just 10 of them)
    private Country mCorrectCountry; // correct country for the current question
    private int mTotalGuesses; // number of total guesses made
    private int mCorrectGuesses; // number of correct guesses
    private SecureRandom rng; // used to randomize the quiz
    private Handler handler; // used to delay loading next country

    private TextView mQuestionNumberTextView; // shows current question #
    private ImageView mFlagImageView; // displays a flag
    private TextView mAnswerTextView; // displays correct answer


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mQuizCountriesList = new ArrayList<>(FLAGS_IN_QUIZ);
        rng = new SecureRandom();
        handler = new Handler();

        // DONE: Get references to GUI components (textviews and imageview)
        mQuestionNumberTextView = findViewById(R.id.questionNumberTextView);
        mFlagImageView = findViewById(R.id.flagImageView);
        mAnswerTextView = findViewById(R.id.answerTextView);

        // DONE: Put all 4 buttons in the array (mButtons)
        mButtons[0] = findViewById(R.id.button);
        mButtons[1] = findViewById(R.id.button2);
        mButtons[2] = findViewById(R.id.button3);
        mButtons[3] = findViewById(R.id.button4);

        // Add 4 more buttons but set their visibility to be GONE
        mButtons[4] = findViewById(R.id.button5);
        mButtons[5] = findViewById(R.id.button6);
        mButtons[6] = findViewById(R.id.button7);
        mButtons[7] = findViewById(R.id.button8);
        updateChoices();

        // DONE: Set mQuestionNumberTextView's text to the appropriate strings.xml resource
        mQuestionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        // DONE: Load all the countries from the JSON file using the JSONLoader
        try {
            mAllCountriesList = JSONLoader.loadJSONFromAsset(this);
        } catch (IOException e) {
            Log.e(TAG, "Error loading from JSON", e);
        }

        // Attach preference listener
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

        // DONE: Call the method resetQuiz() to start the quiz.
        resetQuiz();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up and starts a new quiz.
     */
    public void resetQuiz() {

        // DONE: Reset the number of correct guesses made
        mCorrectGuesses = 0;

        // DONE: Reset the total number of guesses the user made
        mTotalGuesses = 0;

        // DONE: Clear list of quiz countries (for prior games played)
        mQuizCountriesList.clear();

        // DONE: Randomly add FLAGS_IN_QUIZ (10) countries from the mAllCountriesList into the mQuizCountriesList
        Country random;
        while (mQuizCountriesList.size() < FLAGS_IN_QUIZ) {
            random = mAllCountriesList.get(rng.nextInt(mAllCountriesList.size()));
            // DONE: Ensure no duplicate countries (e.g. don't add a country if it's already in mQuizCountriesList)
            if (!mQuizCountriesList.contains(random)
                    && (random.getRegion().equals(mRegion))
                    || mRegion.equals("All"))
            {
                mQuizCountriesList.add(random);
            }
        }

        // DONE: Start the quiz by calling loadNextFlag
        loadNextFlag();
    }

    /**
     * Method initiates the process of loading the next flag for the quiz, showing
     * the flag's image and then 4 buttons, one of which contains the correct answer.
     */
    private void loadNextFlag() {
        // DONE: Initialize the mCorrectCountry by removing the item at position 0 in the mQuizCountries
        mCorrectCountry = mQuizCountriesList.remove(0);

        // DONE: Clear the mAnswerTextView so that it doesn't show text from the previous question
        mAnswerTextView.setText("");

        // DONE: Display current question number in the mQuestionNumberTextView
        mQuestionNumberTextView.setText(getString(
                R.string.question,
                FLAGS_IN_QUIZ - mQuizCountriesList.size(),
                FLAGS_IN_QUIZ));

        // DONE: Use AssetManager to load next image from assets folder
        AssetManager am = getAssets();

        // DONE: Get an InputStream to the asset representing the next flag
        try {
            InputStream stream = am.open(mCorrectCountry.getFileName());

            // DONE: and try to use the InputStream to create a Drawable
            // DONE: The file name can be retrieved from the correct country's file name.
            Drawable image = Drawable.createFromStream(stream, mCorrectCountry.getName());

            // DONE: Set the image drawable to the correct flag.
            mFlagImageView.setImageDrawable(image);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image from file: " + mCorrectCountry.getFileName(), e);
        }

        // DONE: Shuffle the order of all the countries (use Collections.shuffle)
        Collections.shuffle(mAllCountriesList);

        // DONE: Loop through all 4 buttons, enable them all and set them to the first 4 countries
        // DONE: in the all countries list
        for (int i = 0; i < mChoices; i++) {
            mButtons[i].setEnabled(true);
            mButtons[i].setText(mAllCountriesList.get(i).getName());
        }

        // DONE: After the loop, randomly replace one of the 4 buttons with the name of the correct country
        mButtons[rng.nextInt(mChoices)].setText(mCorrectCountry.getName());
    }

    /**
     * Handles the click event of one of the 4 buttons indicating the guess of a country's name
     * to match the flag image displayed.  If the guess is correct, the country's name (in GREEN) will be shown,
     * followed by a slight delay of 2 seconds, then the next flag will be loaded.  Otherwise, the
     * word "Incorrect Guess" will be shown in RED and the button will be disabled.
     * @param v
     */
    public void makeGuess(View v) {

        // DONE: Downcast the View v into a Button (since it's one of the 4 buttons)
        Button clickedButton = (Button) v;

        // DONE: Get the country's name from the text of the button
        String guess = clickedButton.getText().toString();

        // increment the total number of guesses
        mTotalGuesses++;

        // DONE: If the guess matches the correct country's name, increment the number of correct guesses,
        // DONE: then display correct answer in green text.  Also, disable all 4 buttons (can't keep guessing once it's correct)
        if (guess.equals(mCorrectCountry.getName())) {
            // increment the number of correct guesses
            mCorrectGuesses++;

            // disable all 4 buttons (can't keep guessing once it's correct)
            for (int i = 0; i < mChoices; i++) {
                mButtons[i].setEnabled(false);
            }

            // display correct answer in green text
            mAnswerTextView.setTextColor(getResources().getColor(R.color.correct_answer));
            mAnswerTextView.setText(mCorrectCountry.getName());

            if (mCorrectGuesses < FLAGS_IN_QUIZ) {
                // Code a delay (2000ms = 2 seconds) using a handler to load the next flag
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNextFlag();
                    }
                }, 2000);
            }
            
            // DONE: Nested in this decision, if the user has completed all 10 questions, show an AlertDialog
            // DONE: with the statistics and an option to Reset Quiz
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(
                        R.string.results,
                        mTotalGuesses,
                        (double) mCorrectGuesses / mTotalGuesses * 100));

                // Set positive button of the dialog
                // positive button = reset quiz
                builder.setPositiveButton(
                        getString(R.string.reset_quiz),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetQuiz();
                    }
                });

                // To prevent user from getting stuck!
                builder.setCancelable(false);
                builder.create();
                builder.show();
            }
        }

        // DONE: Else, the answer is incorrect, so display "Incorrect Guess!" in red
        // DONE: and disable just the incorrect button.
        else
        {
            // set the clicked button to disabled
            clickedButton.setEnabled(false);

            // set the incorrect text and text color in the TextView
            mAnswerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
            mAnswerTextView.setText(getString(R.string.incorrect_answer));
        }
    }

    /**
     * Responds to the new settings and update the user interface (for buttons) or regions (for
     * flags) accordingly
     */
    SharedPreferences.OnSharedPreferenceChangeListener
    mSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(REGIONS)) {
                String region =
                        sharedPreferences.getString(REGIONS, getString(R.string.default_region));
                updateRegion(region);
                resetQuiz();
            } else if (key.equals(CHOICES)) {
                mChoices =
                        Integer.parseInt(
                                sharedPreferences.getString(
                                        CHOICES,
                                        getString(R.string.default_choices)));
                updateChoices();
                resetQuiz();
            }
            Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Changes the region, replaces the underscore with a space
     * @param region
     */
    private void updateRegion(String region) {
        mRegion = region.replaceAll("_", " ");
        // All, Africa, Asia, Europe, North America, Oceania, South America
    }

    /**
     * Changes the choices
     */
    private void updateChoices() {
        for (int i = 0; i < mChoices; i++) {
            mButtons[i].setVisibility(View.VISIBLE);
        }
        for (int i = mChoices; i < mButtons.length; i++) {
            mButtons[i].setVisibility(View.GONE);
        }
    }
}
