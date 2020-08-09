package edu.dartmouth.cs.scribble.adapters;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.models.Constants;
import edu.dartmouth.cs.scribble.models.Guess;

public class GuessAdapter extends ArrayAdapter<Guess> {
    private Context context;
    private List<Guess> guessList;

    public GuessAdapter(@NonNull Context context, @NonNull List<Guess> objects) {
        super(context, 0, objects);
        this.context = context;
        guessList = objects;
    }

    @Override
    public int getCount() {
        return guessList.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Guess guess = guessList.get(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.guess_item, parent, false);
        }

        TextView guessView = convertView.findViewById(R.id.guessed_view);
        TextView nameView = convertView.findViewById(R.id.name_view);

        if (guess.getGuessType() == Constants.GUESS_TYPE_CORRECT) {
            nameView.setVisibility(View.INVISIBLE);
            nameView.setText("");
            guessView.setVisibility(View.VISIBLE);
            guessView.setText(guess.getPlayerName() + " guessed the word!");
        }
        else {
            guessView.setVisibility(View.INVISIBLE);
            guessView.setText("");
            nameView.setVisibility(View.VISIBLE);
            nameView.setText("");

            SpannableString ss = new SpannableString(guess.getPlayerName());
            ss.setSpan(new StyleSpan(Typeface.BOLD), 0, ss.length(), 0);
            nameView.append(ss);
            nameView.append(": ");
            nameView.append(guess.getGuess());

            //nameView.setText(guess.getPlayerName() + ": " + guess.getGuess());
            //wordView.setText(guess.getGuess().replace(" ", "\u00A0")); // Wrap by character.
        }

        return convertView;
    }
}
