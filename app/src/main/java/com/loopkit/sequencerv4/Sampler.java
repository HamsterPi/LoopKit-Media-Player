package com.loopkit.sequencerv4;

import android.content.Context;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import android.os.Build;

import android.util.Log;

public class Sampler  {
    SoundPool sp;
    private int kickId;
    private int hhId;
    private int snareId;
    private int percId;
    AudioAttributes attr;
    private Context mycontext;

    public Sampler(Context appcontext){
        Log.d("Sampler Class", "Create Sampler");
        try{

            this.mycontext = appcontext;

            // Parts of SoundPool are deprecated from Lollipop so we need to check if it requires the use of a newer SoundPool constructor
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP){

                attr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).
                                setUsage(AudioAttributes.USAGE_MEDIA).
                                build();
                sp = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attr).build();

            }

            // If version is lower proceed with older SoundPool constructor
            else{
                sp = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
            }


            Log.d("Loading", "Sounds");
            // Loading Sounds from res/raw directory
            kickId = sp.load(mycontext, R.raw.kick , 1);
            snareId = sp.load(mycontext, R.raw.snare, 1);
            percId = sp.load(mycontext, R.raw.perc, 1);
            hhId = sp.load(mycontext, R.raw.hh,1);

            Log.d("Sampler", "Initiated");

        }
        catch (Exception e){
            Log.d("Exception", "Occurred", e);
        }
    }

    // Play specified drum sample
    public void play(int[] curStep){

        for (int i = 1; i<5;i++){

            if (curStep[i] ==1){
                sp.play(returnDrum(i), 1, 1, 1, 0, 1);
            }
        }
    }

    // Returns drum based on row
    public int returnDrum(int drum){
        switch (drum){
            case 1:
                drum=kickId;
                break;
            case 2:
                drum =hhId;
                break;
            case 3:
                drum = snareId;
                break;
            case 4:
                drum = percId;
                break;

        }
        return drum;
    }

}