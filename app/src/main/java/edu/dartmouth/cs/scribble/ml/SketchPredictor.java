package edu.dartmouth.cs.scribble.ml;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Set;

import edu.dartmouth.cs.scribble.R;
import edu.dartmouth.cs.scribble.models.Constants;

public class SketchPredictor {
    private FirebaseCustomRemoteModel remoteModel;
    private FirebaseModelInterpreter interpreter;
    private FirebaseModelInputOutputOptions inputOutputOptions;
    private Resources resources;
    private Context context;
    private float[] probArray;
    private Set<String> alreadyGuessed;

    public SketchPredictor(Context context) {
        resources = context.getResources();
        this.context = context;
        initModel();
        initOptions();
        alreadyGuessed = new HashSet<>();
    }

    public boolean isInitialized() {
        return remoteModel != null && interpreter != null && inputOutputOptions != null;
    }

    private void initModel() {
        remoteModel =
                new FirebaseCustomRemoteModel.Builder(Constants.MODEL_NAME).build();

        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Success.
                        FirebaseModelInterpreterOptions options;
                        options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
                        try {
                            interpreter = FirebaseModelInterpreter.getInstance(options);
                        } catch (FirebaseMLException e) {
                            e.printStackTrace();
                        }

                        initOptions();
                    }
                });
    }

    private void initOptions() {
        try {
            inputOutputOptions =
                    new FirebaseModelInputOutputOptions.Builder()
                            .setInputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, 28, 28, 1})
                            .setOutputFormat(0, FirebaseModelDataType.FLOAT32, new int[]{1, Constants.NUM_CLASSES})
                            .build();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }
    }

    // Given a black and white (black background) square bitmap, sets a float array of probabilities
    // that correspond to the array stored in Constants.
    public void makeInference(Bitmap bitmap) {
        //Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.moon);
        bitmap = Bitmap.createScaledBitmap(bitmap, 28, 28, true);
        Log.d("airunnable", "makeinference");
        /*
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = bitmap.getRowBytes() * bitmap.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        bitmap.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
         */

        int batchNum = 0;
        float[][][][] input = new float[1][28][28][1];
        for (int x = 0; x < 28; x++) {
            for (int y = 0; y < 28; y++) {
                int pixel = bitmap.getPixel(x, y);
                // Normalize channel values to [-1.0, 1.0]. This requirement varies by
                // model. For example, some models might require values to be normalized
                // to the range [0.0, 1.0] instead.

                int color = (Color.red(pixel) + Color.blue(pixel) + Color.green(pixel)) / 3;
                Log.d("color", String.valueOf(color));

                input[batchNum][x][y][0] = (color / 255.0f);
                //input[batchNum][x][y][0] = ((color - 127.5f) / 127.5f);

                /*
                if (color < 250) {
                    input[batchNum][x][y][0] = 0;
                }
                else {
                    input[batchNum][x][y][0] = 1;
                }
                */
                //input[batchNum][x][y][0] = color;
                Log.d("pixel " + String.valueOf(x) + " " + String.valueOf(y), String.valueOf(color));
            }
        }

        FirebaseModelInputs inputs = null;
        try {
            inputs = new FirebaseModelInputs.Builder()
                    .add(input)  // add() as many input arrays as your model requires
                    .build();
        } catch (FirebaseMLException e) {
            e.printStackTrace();
        }

        interpreter.run(inputs, inputOutputOptions)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseModelOutputs>() {
                            @Override
                            public void onSuccess(FirebaseModelOutputs result) {
                                Log.d("airunnable", "success");
                                float[][] output = result.getOutput(0);
                                probArray = output[0];
                                for (int i = 0; i < probArray.length; i++) {
                                    Log.d("output " + i, String.valueOf(probArray[i]));
                                }
                                broadcastGuess();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
    }

    public void clearAlreadyGuessed() {
        alreadyGuessed = new HashSet<>();
    }

    // Broadcasts the most likely word.
    private void broadcastGuess() {
        float prob = 0;
        String word = "";
        for (int i = 0; i < probArray.length; i++) {
            if (probArray[i] > prob) {
                if (!alreadyGuessed.contains(Constants.ML_WORDS[i])) {
                    prob = probArray[i];
                    word = Constants.ML_WORDS[i];
                }
                else if (i > 0 && !alreadyGuessed.contains(Constants.ML_WORDS[i - 1])) {
                    prob = probArray[i];
                    word = Constants.ML_WORDS[i - 1];
                }
            }
        }

        if (word.length() > 0) {
            alreadyGuessed.add(word);

            Intent intent = new Intent(Constants.BROADCAST_GUESS);
            intent.putExtra(Constants.GUESS_KEY, word);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(28*28*4);
        byteBuffer.order(ByteOrder.nativeOrder());

        for (int x = 0; x < 28; x++) {
            for (int y = 0; y < 28; y++) {
                int pixel = bitmap.getPixel(x, y);
                byteBuffer.putFloat(((pixel & 0xFF)));
            }
        }
        return byteBuffer;
    }
}
