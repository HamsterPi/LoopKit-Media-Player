package com.loopkit.sequencerv4;

import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;

import android.util.Log;

import android.Manifest;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioRecord;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.lang.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private int beats_per_minute = 120;
    private int max_bpm = 210;
    private int min_bpm = 30;
    private int seq_row = 1;
    private int seq_column = 0;


    private Button inc_bpm;
    private Button sub_bpm;

    private ToggleButton step1, step2, step3, step4, step5, step6, step7, step8, step9, step10, step11, step12, step13, step14, step15, step16;

    private ToggleButton kick, snare, hh, perc;
    private Button record1, record2, record3, record4;
    private ImageButton play_seq;

    private TextView bpmLabel;
    private Sequencer mySeq;

    private ToggleButton[] stepArray;
    private List curDrumSeq;

    //start of loop audio part
    private static final int RECORDER_SAMPLERATE = 44100; //44100 is standard
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final String TAG = "LoopKit";
    private static final int RECORDER_CHANNELS_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;

    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    public boolean playing = false; //toggle
    public int drum_position = 0;

    public boolean layer1setup = false; //toggle
    public boolean layer2setup = false; //toggle
    public boolean layer3setup = false; //toggle
    public boolean layer4setup = false; //toggle

    public boolean layer1playing = false; //toggle
    public boolean layer2playing = false; //toggle
    public boolean layer3playing = false; //toggle
    public boolean layer4playing = false; //toggle

    public boolean layer1mute = false;
    public boolean layer2mute = false;
    public boolean layer3mute = false;
    public boolean layer4mute = false;


    //start waiting time at 0
    public int waitingTimea = 0;
    public int waitingTimeb = 0;

    MediaPlayer mediaPlayer1a = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class
    MediaPlayer mediaPlayer1b = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class

    MediaPlayer mediaPlayer2a = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class
    MediaPlayer mediaPlayer2b = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class

    MediaPlayer mediaPlayer3a = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class
    MediaPlayer mediaPlayer3b = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class

    MediaPlayer mediaPlayer4a = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class
    MediaPlayer mediaPlayer4b = new MediaPlayer(); //media player instance is created externally now so it can be paused within the pause class


    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    private Object PendingIntent;

    private boolean FilePermissions() { //GET PERMISSIONS
        int permissionWRITE_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionRECORD_AUDIO = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        List<String> permissionList = new ArrayList<>();

        if (permissionWRITE_EXTERNAL_STORAGE != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissionRECORD_AUDIO != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(android.Manifest.permission.RECORD_AUDIO);
        }

        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }


    public class MyRunnable implements Runnable {

        private int var;

        public MyRunnable(int var) {
            this.var = var;
        }

        public void run() {
            // code in the other thread, can reference "var" variable
        }
    }


    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!FilePermissions()) {
            return;
        }

        //pop up box to tell people how to use loopkit
        AlertDialog alertDialog = new AlertDialog.Builder(this)


                //.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Welcome to LoopKit!")
                .setMessage("-Increase/decrease the bpm of the drum sequencer by tapping left or right\n" +
                        "-Select a drum instrument with the drum buttons, then tap on one of the 16 rectangles to apply/remove the beat\n" +
                        "-Record a layer (starting with layer1) by tapping record when the drum machine is playing\n" +
                        "-Pause and play again to hear the added recorded loop\n" +
                        "-Tapping record1/2/3/4 while playing will allow you to write/overwrite that layer\n" +
                        "-Tapping record1/2/3/4 while paused will mute/unmute that layer\n" +
                        "-Tapping guide will re-display this message\n"+
                        "-Tapping clear will quit the program and clear all loop/sequencer data")

                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bpmLabel = (TextView) findViewById(R.id.bpmNumber);
        setBpmLabel();

        inc_bpm = (Button) findViewById(R.id.inc_bpm);
        sub_bpm = (Button) findViewById(R.id.sub_bpm);

        play_seq = (ImageButton) findViewById(R.id.play);


        handler = new Handler();

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        // Setting Up BPM stuff.
        View.OnClickListener bpmListener = new View.OnClickListener() {

            public void onClick(View v) {
                Button b = (Button) v;


                if (b.getText().toString().equals("+") && beats_per_minute < max_bpm) {
                    beats_per_minute++;
                }
                if (b.getText().toString().equals("-") && beats_per_minute > min_bpm) {
                    beats_per_minute--;

                }

                setBpmLabel();
                Log.d("Bpm", String.valueOf(beats_per_minute));
            }
        };

        // Changing Bpm number By 10
        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Button b = (Button) v;

                if (b.getText().toString().equals("+") && beats_per_minute < max_bpm - 10) {
                    beats_per_minute = beats_per_minute + 10;
                }
                if (b.getText().toString().equals("-") && beats_per_minute > min_bpm + 10) {
                    beats_per_minute = beats_per_minute - 10;

                }

                setBpmLabel();
                mySeq.setBeats_per_minute(beats_per_minute);
                Log.d("Bpm", String.valueOf(beats_per_minute));
                return true;
            }

        };

        View.OnLongClickListener longClickListener2 = new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {


                return true;
            }

        };
        inc_bpm.setOnClickListener(bpmListener);
        sub_bpm.setOnClickListener(bpmListener);
        inc_bpm.setOnLongClickListener(longClickListener);
        sub_bpm.setOnLongClickListener(longClickListener);

        final Handler myHandler = new Handler();

        //creating a sequence
        mySeq = new Sequencer(MainActivity.this, this.getApplicationContext());

        stepArray = new ToggleButton[]{step1, step2, step3, step4, step5, step6, step7, step8,
                step9, step10, step11, step12, step13, step16, step15, step16};

        Resources res = getResources();
        TypedArray ids = res.obtainTypedArray(R.array.id);

        // StepSequencer ToggleButtons init
        for (int i = 0; i < 16; i++) {

            stepArray[i] = (ToggleButton) findViewById(ids.getResourceId(i, i));
        }

        //Listener
        View.OnClickListener stepListener = new View.OnClickListener() {

            public void onClick(View v) {
                ToggleButton t = (ToggleButton) v;
                seq_column = Integer.parseInt(t.getTag().toString());


                if (t.isChecked()) {
                    mySeq.changestep(seq_column, seq_row, 1); //calling Seq method
                } else {
                    mySeq.changestep(seq_column, seq_row, 0);
                }
                mySeq.outputSequence();


                Log.d("this is step number!", " " + t.getTag());
            }
        };


        //setting stepListener
        for (int i = 0; i < 16; i++) {

            stepArray[i].setOnClickListener(stepListener);
        }

        // Drum & seq_row choosing Setup
        kick = (ToggleButton) findViewById(R.id.kick);
        snare = (ToggleButton) findViewById(R.id.snare);
        hh = (ToggleButton) findViewById(R.id.hh);
        perc = (ToggleButton) findViewById(R.id.perc);

        //
        record1 = (Button) findViewById(R.id.record1);
        record2 = (Button) findViewById(R.id.record2);
        record3 = (Button) findViewById(R.id.record3);
        record4 = (Button) findViewById(R.id.record4);

        View.OnClickListener drumListener = new View.OnClickListener() {

            public void onClick(View v) {
                ToggleButton t = (ToggleButton) v;
                seq_row = Integer.parseInt(t.getTag().toString());
                Log.d("Row is", "" + t.getTag());//debugging purpose
                drumToggles(seq_row);
                mySeq.outputSequence();//debugging purpose
                restoreStepsForDrum();

            }
        };

        kick.setOnClickListener(drumListener);
        snare.setOnClickListener(drumListener);
        hh.setOnClickListener(drumListener);
        perc.setOnClickListener(drumListener);

    }
    //END OF ON CREATE



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /// restoring ToggleButton steps for a specific drum from
    //sequence
    private void restoreStepsForDrum() {
        List sqnc = mySeq.returnSequenceForDrum(seq_row);
        for (int i = 0; i < 16; i++) {
            int z = (int) sqnc.get(i);
            //Log.d("Toledo", String.valueOf(z)); ///////////////////////////////////////
            switch (z) {
                case 1:
                    stepArray[i].setChecked(true);
                    break;
                case 0:
                    stepArray[i].setChecked(false);
            }

        }

    }

    // set beats_per_minute
    private void setBpmLabel() {
        bpmLabel.setText(String.valueOf(beats_per_minute));
    }

    // drum choice toggle management
    private void drumToggles(int drumRow) {
        switch (drumRow) {
            case 1:
                kick.setChecked(true);
                hh.setChecked(false);
                snare.setChecked(false);
                perc.setChecked(false);
                break;
            case 2:
                kick.setChecked(false);
                hh.setChecked(true);
                snare.setChecked(false);
                perc.setChecked(false);
                break;
            case 3:
                kick.setChecked(false);
                hh.setChecked(false);
                snare.setChecked(true);
                perc.setChecked(false);
                break;
            case 4:
                kick.setChecked(false);
                hh.setChecked(false);
                snare.setChecked(false);
                perc.setChecked(true);
                break;
        }

    }


    //////////////////////play media recorder
    public void play(View view) throws IOException {

        //drum loop
        sequenceloop(view);
        //audio loop
        audioloop(view);

    }

    public void sequenceloop(View view) {
        Log.d("Playing", "The Sequence");
        playing = true;

        //drum sequence thread
        Thread myThread1 = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TimeUnit.MILLISECONDS.sleep(275); //pause to wait for the audio loop to start
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mySeq.play();


                handler.post(new Runnable() {
                    @Override
                    public void run() {

                    }
                });


            }
        });
        myThread1.start();

        //drum position tracker thread
        Thread myThread2 = new Thread(new Runnable() {
            @Override
            public void run() {

                //start timer counts to length of the the loop dependant on bpm, then resets to 0 when done public timer

                //Log.d(TAG, "start");
                while (playing) {

                    try {
                        //Log.d("toledo", String.valueOf(drum_position));
                        TimeUnit.MILLISECONDS.sleep(1);
                        drum_position += 1;

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (drum_position == ((60000 / (beats_per_minute * 4)) * 16) + 1400) //1400 buffer for the time it takes for the beats themselves to execute
                    {
                        drum_position = 0;
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });


                }

            }

        });
        myThread2.start();


        if (myThread1.isAlive()) {
            play_seq.setClickable(false);
        }




    }


    public void audioloop(View view) throws IOException {


        //Log.d(TAG, "start");
        try {

            if (layer1setup) {

                //set up the 2 mediaplayers needed for one layer of the loop
                mediaPlayer1a.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording1.wav");
                mediaPlayer1a.prepare();
                mediaPlayer1b.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording1.wav");
                mediaPlayer1b.prepare();
                //their own looping does not suffice, we're doing it our own way as you will see below
                mediaPlayer1a.setLooping(false);
                mediaPlayer1b.setLooping(false);
                mediaPlayer1a.start();
                layer1playing = true;
            }

            if (layer2setup) {

                mediaPlayer2a.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording2.wav");
                mediaPlayer2a.prepare();
                mediaPlayer2b.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording2.wav");
                mediaPlayer2b.prepare();
                //their own looping does not suffice, we're doing it our own way as you will see below
                mediaPlayer2a.setLooping(false);
                mediaPlayer2b.setLooping(false);

                //start playing layers using mediaplayer
                mediaPlayer2a.start();
                layer2playing = true;

            }

            if (layer3setup) {

                mediaPlayer3a.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording3.wav");
                mediaPlayer3a.prepare();
                mediaPlayer3b.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording3.wav");
                mediaPlayer3b.prepare();
                //their own looping does not suffice, we're doing it our own way as you will see below
                mediaPlayer3a.setLooping(false);
                mediaPlayer3b.setLooping(false);

                //start playing layers using mediaplayer
                mediaPlayer3a.start();
                layer3playing = true;

            }

            if (layer4setup) {

                mediaPlayer4a.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording4.wav");
                mediaPlayer4a.prepare();
                mediaPlayer4b.setDataSource(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording4.wav");
                mediaPlayer4b.prepare();
                //their own looping does not suffice, we're doing it our own way as you will see below
                mediaPlayer4a.setLooping(false);
                mediaPlayer4b.setLooping(false);

                //start playing layers using mediaplayer
                mediaPlayer4a.start();
                layer4playing = true;

            }

            //alternate to mediaplayer 2 before mediaplayer 1 has technically finished to account for the time it takes the player to start playing
            Timer HACK_loopTimera = new Timer(); //timer1
            TimerTask HACK_loopTaska = new TimerTask()
            {
                @Override public void run() {

                    if (layer1setup) {
                        mediaPlayer1b.start();
                    }
                    if (layer2setup){
                        mediaPlayer2b.start();
                    }
                    if (layer3setup){
                        mediaPlayer3b.start();
                    }
                    if (layer4setup){
                        mediaPlayer4b.start();
                    }
                }
            };

            //starts about 350 milliseconds early to account for delay
            //however because the time it takes varies some fallout is inevitable

            Log.d("Toledo", "duration is: " + mediaPlayer1a.getDuration());
            waitingTimea = (mediaPlayer1a.getDuration() - 275);
            HACK_loopTimera.schedule(HACK_loopTaska, waitingTimea, waitingTimea);


            //alternate back to mediaplayer a before mediaplayer b has technically finished...and so on

            Timer HACK_loopTimerb = new Timer(); //timer2
            TimerTask HACK_loopTaskb = new TimerTask()
            {
                @Override public void run() {
                    if (layer1setup) {
                        mediaPlayer1a.start();
                    }
                    if (layer2setup){
                        mediaPlayer2a.start();
                    }
                    if (layer3setup){
                        mediaPlayer3a.start();
                    }
                    if (layer4setup){
                        mediaPlayer4a.start();
                    }
                }
            };

            waitingTimeb = (mediaPlayer1b.getDuration() - 275);
            HACK_loopTimerb.schedule(HACK_loopTaskb, waitingTimeb, waitingTimeb);





            //Thread to check if the loop has been paused and if so cancel the timer
            Thread myThread3 = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (playing) {

                        try {
                            TimeUnit.MILLISECONDS.sleep(1);

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        //cancel the timer when paused
                        if (playing == false)
                        {
                            HACK_loopTimera.cancel();
                            waitingTimea = 0;
                            HACK_loopTimerb.cancel();
                            waitingTimeb = 0;
                        }


                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });


                    }

                }

            });
            myThread3.start();




        } catch (Exception e) {
            Log.d(TAG, "error");
        }

    }



//End of audioloop

    public void guide(View view) throws IOException {


        //displays the guide to how to use the app
        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Welcome to LoopKit!")
                .setMessage("-Increase/decrease the bpm of the drum sequencer by tapping left or right\n" +
                        "-Select a drum instrument with the drum buttons, then tap on one of the 16 rectangles to apply/remove the beat\n" +
                        "-Record a layer (starting with layer1) by tapping record when the drum machine is playing\n" +
                        "-Pause and play again to hear the added recorded loop\n" +
                        "-Tapping record1/2/3/4 while playing will allow you to write/overwrite that layer\n" +
                        "-Tapping record1/2/3/4 while paused will mute/unmute that layer\n" +
                        "-Tapping guide will re-display this message\n"+
                        "-Tapping clear will quit the program and clear all loop/sequencer data")

                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                })
                .show();
    }

    public void clear(View view) throws IOException {

        //exits the program and clears any loop/sequencer data
        System.exit(0);

    }



    public void stop(View view) throws IOException {

        //stop the drum
        stopDrumSeq(view);
        //stop the audio loop
        stopAudioSeq(view);

    }
    public void stopDrumSeq(View view) {

        //stop the drum sequence
        mySeq.stop();
        //make the play button active
        play_seq.setClickable(true);

    }

    public void stopAudioSeq(View view) throws IOException {


        //only stopping that which is actually playing, not just setup
        Log.d(TAG, "stopSeq2");
        Log.d(TAG, "layer 1" + layer1playing);

        try {
            //if the layer has been recorded and is playing, stop it when we press the stop button
            if (layer1setup & layer1playing) {
                mediaPlayer1a.stop();
                mediaPlayer1a.reset(); //allows the loop to be played again
                mediaPlayer1b.stop();
                mediaPlayer1b.reset(); //allows the loop to be played again
            }

            if (layer2setup & layer2playing) {
                mediaPlayer2a.stop();
                mediaPlayer2a.reset(); //allows the loop to be played again
                mediaPlayer2b.stop();
                mediaPlayer2b.reset(); //allows the loop to be played again
            }

            if (layer3setup & layer3playing) {
                mediaPlayer3a.stop();
                mediaPlayer3a.reset(); //allows the loop to be played again
                mediaPlayer3b.stop();
                mediaPlayer3b.reset(); //allows the loop to be played again
            }

            if (layer4setup & layer4playing) {
                mediaPlayer4a.stop();
                mediaPlayer4a.reset(); //allows the loop to be played again
                mediaPlayer4b.stop();
                mediaPlayer4b.reset(); //allows the loop to be played again
            }


            //Log.d("Sequence", "Stopped");


        }

        catch (IllegalStateException e) {
            e.printStackTrace();
        }

        //update necessary values now that we have paused.
        drum_position = 0;
        playing = false;
        layer1playing = false;
        layer2playing = false;
        layer3playing = false;
        layer4playing = false;


    }

    //record the first layer of audio
    public void record1func(View view) throws InterruptedException {

        //if the layer is set up but not playing then we mute/unmute on touch
        if (!layer1playing & layer1setup) {
            Log.d(TAG, "layer1playing: " + layer1playing);
            Log.d(TAG, "layer1setup: " + layer1setup);

            if (!layer1mute) {
                Toast.makeText(getApplicationContext(), "muted layer 1", Toast.LENGTH_LONG).show();
                mediaPlayer1a.setVolume(0, 0);
                mediaPlayer1b.setVolume(0, 0);
                layer1mute = true;

            }

            else
            {
                Toast.makeText(getApplicationContext(), "unmuted layer 1", Toast.LENGTH_LONG).show();
                mediaPlayer1a.setVolume(1, 1);
                mediaPlayer1b.setVolume(1, 1);
                layer1mute = false;
            }


        }

        //only start recording if the drum machine is playing (as we need to sync with it)

        //Log.d(TAG, "playing = " + playing);
        if (playing) {

            try {

                //needs to start at the start of the drum machine line
                //wait till we're at the start of the next drum loop

                Log.d(TAG, "record pressed at: " + drum_position);//drum position on time of record press

                //get the bpm value to calculate wait time
                //record for the exact length of the drum loop
                int waittime = (((60000/(beats_per_minute*4)) * 16)) - drum_position - 275;
                TimeUnit.MILLISECONDS.sleep(waittime);

                //Log.d(TAG, "recording 1 started at: " + drum_position); //possibly takes 50ms to start..also device speed may fuck this up

                startRecording1();
                Log.d("Toledo", "recording started at..." + drum_position);
                int recordtime = ( ( (60000/(beats_per_minute*4) ) * 16) + 275);
                TimeUnit.MILLISECONDS.sleep(recordtime);
                //Log.d(TAG, "Recording stopped: " + recordtime);
                stopRecording1();

                layer1setup = true;


            } catch (IllegalStateException ise) {
                // Exception
            }

        }


    }

    public void record2func(View view) throws InterruptedException {


        //if the layer is set up but not playing then we mute/unmute on touch

        //Log.d(TAG, "layer2playing: " + layer2playing);
        //Log.d(TAG, "layer2setup: " + layer2setup);
        if (!layer2playing & layer2setup) {

            if (!layer2mute) {
                Toast.makeText(getApplicationContext(), "muted layer 2", Toast.LENGTH_LONG).show();
                mediaPlayer2a.setVolume(0, 0);
                mediaPlayer2b.setVolume(0, 0);
                layer2mute = true;
            }
            else
            {
                Toast.makeText(getApplicationContext(), "unmuted layer 2", Toast.LENGTH_LONG).show();
                mediaPlayer2a.setVolume(1, 1);
                mediaPlayer2b.setVolume(1, 1);
                layer2mute = false;
            }


        }

        //only start recording if the drum machine is playing (as we need to sync with it)

        //Log.d(TAG, "playing = " + playing);
        if (playing) {

            //set up waittime for the second layer
            int waittime2;

            try {

                //needs to start at the start of the next loop
                //wait till we're at the start of the next loop, depending on if mediaplayer1a is playing or mediaplayer1b, using those as mediaplayer1 is first to be recorded

                if(mediaPlayer1a.isPlaying()){
                    waittime2 = ((60000/(beats_per_minute*4)) * 16) - mediaPlayer1a.getCurrentPosition() - 275; //
                }

                else
                {
                    waittime2 = ((60000 / (beats_per_minute * 4)) * 16) - mediaPlayer1b.getCurrentPosition() - 275; //
                }


                TimeUnit.MILLISECONDS.sleep(waittime2);

                startRecording2();
                Log.d("Toledo", "recording started at..." + drum_position);
                int recordtime = ( ( (60000/(beats_per_minute*4) ) * 16) + 275);
                TimeUnit.MILLISECONDS.sleep(recordtime);
                //Log.d(TAG, "Recording stopped: " + recordtime);
                stopRecording2();

                layer2setup = true;


            } catch (IllegalStateException ise) {
                // Exception
            }

        }

    }

    public void record3func(View view) throws InterruptedException {

        //if the layer is set up but not playing then we mute/unmute on touch

        //Log.d(TAG, "layer3playing: " + layer3playing);
        //Log.d(TAG, "layer3playing: " + layer3setup);
        if (!layer3playing & layer3setup) {

            if (!layer3mute) {
                Toast.makeText(getApplicationContext(), "muted layer 3", Toast.LENGTH_LONG).show();
                mediaPlayer3a.setVolume(0, 0);
                mediaPlayer3b.setVolume(0, 0);
                layer3mute = true;
            }
            else
            {
                Toast.makeText(getApplicationContext(), "unmuted layer 3", Toast.LENGTH_LONG).show();
                mediaPlayer3a.setVolume(1, 1);
                mediaPlayer3b.setVolume(1, 1);
                layer3mute = false;
            }


        }

        //only start recording if the drum machine is playing (as we need to sync with it)

        //Log.d(TAG, "playing = " + playing);
        if (playing) {

            int waittime3;

            try {

                //needs to start at the start of the next loop
                //wait till we're at the start of the next loop, depending on if mediaplayer1a is playing or mediaplayer1b, using those as mediaplayer1 is first to be recorded

                if(mediaPlayer1a.isPlaying()){
                    waittime3 = ((60000/(beats_per_minute*4)) * 16) - mediaPlayer1a.getCurrentPosition() - 275; //
                }

                else
                {
                    waittime3 = ((60000 / (beats_per_minute * 4)) * 16) - mediaPlayer1b.getCurrentPosition() - 275; //
                }


                TimeUnit.MILLISECONDS.sleep(waittime3);

                startRecording3();

                //Log.d("Toledo", "recording started at..." + drum_position);


                int recordtime = ( ( (60000/(beats_per_minute*4) ) * 16) + 275);
                TimeUnit.MILLISECONDS.sleep(recordtime);

                Log.d(TAG, "Recording2 stopped: " + recordtime);
                stopRecording3();


                layer3setup = true;

            } catch (IllegalStateException ise) {
                // Exception
            }


        }


    }

    public void record4func(View view) throws InterruptedException {

        //if the layer is set up but not playing then we mute/unmute on touch
        //Log.d(TAG, "layer4playing: " + layer4playing);
        //Log.d(TAG, "layer4setup: " + layer4setup);
        if (!layer4playing & layer4setup) {

            if (!layer4mute) {
                Toast.makeText(getApplicationContext(), "muted layer 4", Toast.LENGTH_LONG).show();
                mediaPlayer4a.setVolume(0, 0);
                mediaPlayer4b.setVolume(0, 0);
                layer4mute = true;
            }
            else
            {
                Toast.makeText(getApplicationContext(), "unmuted layer 4", Toast.LENGTH_LONG).show();
                mediaPlayer4a.setVolume(1, 1);
                mediaPlayer4b.setVolume(1, 1);
                layer4mute = false;
            }


        }

        //only start recording if the drum machine is playing (as we need to sync with it)

        //Log.d(TAG, "playing = " + playing);
        if (playing) {

            int waittime4;

            try {

                //needs to start at the start of the next loop
                //wait till we're at the start of the next loop, depending on if mediaplayer1a is playing or mediaplayer1b, using those as mediaplayer1 is first to be recorded

                if(mediaPlayer1a.isPlaying()){
                    waittime4 = ((60000/(beats_per_minute*4)) * 16) - mediaPlayer1a.getCurrentPosition() - 275;
                }

                else
                {
                    waittime4 = ((60000 / (beats_per_minute * 4)) * 16) - mediaPlayer1b.getCurrentPosition() - 275;
                }


                TimeUnit.MILLISECONDS.sleep(waittime4);

                startRecording4();

                //Log.d("Toledo", "recording started at..." + drum_position);


                int recordtime = ( ( (60000/(beats_per_minute*4) ) * 16) + 275);
                TimeUnit.MILLISECONDS.sleep(recordtime);

                Log.d(TAG, "Recording2 stopped: " + recordtime);
                stopRecording4();

                layer4setup = true;

            } catch (IllegalStateException ise) {
                // Exception
            }


        }


    }



    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording1() { //calls write to file for layer 1

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile1();
                //Toast.makeText(getApplicationContext(), "writeaudiodata", Toast.LENGTH_LONG).show();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void startRecording2() { //calls write to file for layer 2

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile2();
                //Toast.makeText(getApplicationContext(), "writeaudiodata", Toast.LENGTH_LONG).show();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void startRecording3() { //calls write to file for layer 1

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile3();
                //Toast.makeText(getApplicationContext(), "writeaudiodata", Toast.LENGTH_LONG).show();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }

    private void startRecording4() { //calls write to file for layer 1

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile4();
                //Toast.makeText(getApplicationContext(), "writeaudiodata", Toast.LENGTH_LONG).show();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

    }


    private void writeAudioDataToFile1() { //calls short to byte
        // Write the output audio in byte

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording1.pcm"; //where we writing
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
            // Toast.makeText(getApplicationContext(), "writing to file", Toast.LENGTH_LONG).show();
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = shorttobyte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAudioDataToFile2() { //calls short to byte for layer 2
        // Write the output audio in byte

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording2.pcm"; //where we writing
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
            // Toast.makeText(getApplicationContext(), "writing to file", Toast.LENGTH_LONG).show();
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = shorttobyte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeAudioDataToFile3() { //calls short to byte
        // Write the output audio in byte

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording3.pcm"; //where we writing
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
            // Toast.makeText(getApplicationContext(), "writing to file", Toast.LENGTH_LONG).show();
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = shorttobyte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void writeAudioDataToFile4() { //calls short to byte
        // Write the output audio in byte

        String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording4.pcm"; //where we writing
        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
            // Toast.makeText(getApplicationContext(), "writing to file", Toast.LENGTH_LONG).show();
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = shorttobyte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] shorttobyte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void stopRecording1() {
        // stops the recording activity
        if (null != recorder) { //huh
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            File f1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording1.pcm"); //loc of pcm file
            File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording1.wav"); // The location where you want your WAV file
            try {
                rawToWav(f1, f2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(), "stopped recording", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording2() {
        // stops the recording activity
        if (null != recorder) { //huh
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            File f1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording2.pcm"); //loc of pcm file
            File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording2.wav"); // The location where you want your WAV file
            try {
                rawToWav(f1, f2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(), "stopped recording", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording3() {
        // stops the recording activity
        if (null != recorder) { //huh
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            File f1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording3.pcm"); //loc of pcm file
            File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording3.wav"); // The location where you want your WAV file
            try {
                rawToWav(f1, f2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(), "stopped recording", Toast.LENGTH_LONG).show();
        }
    }

    private void stopRecording4() {
        // stops the recording activity
        if (null != recorder) { //huh
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            File f1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording4.pcm"); //loc of pcm file
            File f2 = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording4.wav"); // The location where you want your WAV file
            try {
                rawToWav(f1, f2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(), "stopped recording", Toast.LENGTH_LONG).show();
        }
    }


    //convert from pcm to wav by adding a header, allowing us to use mediarecorder to play the file
    //calls fully read file to byte
    private void rawToWav(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAV header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size

            // Audio data
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }


    protected void onPause(){


        super.onPause();
        mySeq.stop();


    }

    protected void onDestroy(){
        super.onDestroy();

    }
}