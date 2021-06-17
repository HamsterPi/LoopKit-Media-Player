package com.loopkit.sequencerv4;

import android.content.Context;

import java.util.ArrayList;

import android.util.Log;

import java.lang.*;

public class Sequencer {

    int[][] Sequence;
    Sampler mySampler;
    private Context mycontext;
    public boolean playing;
    private int beats_per_minute;
    private int step;
    private MainActivity mainAct;

    public Sequencer(MainActivity act, Context context) {
        this.mycontext = context;

        // Set default BPM to 120
        beats_per_minute = 120;
        mainAct = act;
        playing = true;

        Sequence = new int[16][5];
        for (int i = 0; i<16; i++){
            Sequence[i] = new int[]{1, 0, 0, 0, 0};
        }
        outputSequence();
        mySampler  = new Sampler(mycontext);

    }

    // Change value of current step
    public void changestep(int row, int column, int value){
        Sequence[row][column] = value;
    }

    // Take row and return it's drum sequence
    public ArrayList returnSequenceForDrum(int row){
        ArrayList<Integer> drumsequence = new ArrayList<>();


        for (int i = 0; i<16; i++){
            int j = Sequence[i][row];
            drumsequence.add(j);
        }

        Log.d("drumsequence",""+drumsequence);
        return drumsequence;
    }

    // Determine audio loop step time
    public void play(){
        playing = true;

        while(playing == true) {
            for (int i = 0; i < 16; i++) {

                // Keep track of when i is zero
                mySampler.play(Sequence[i]);
                step = i;
                if (playing == false) {
                    break;
                }
                try {
                    // Sleep for milliseconds in between each step depending on BPM rate
                    Thread.sleep(60000 / (beats_per_minute * 4));

                    if (i == 15) i = -1;

                } catch (InterruptedException e) {
                e.printStackTrace();
                }
            }
        }
    }

    // Return current step
    public int returnStep() {
        return step;
    }

    public void stop(){

        playing = false;

    }
    // Determining BPM for sequence
    public void setBeats_per_minute(int bpmIn){
        beats_per_minute = bpmIn;
    }

    public void outputSequence(){

        for (int j = 0; j<16; j++){
            System.out.println("Step"+ j);

            for (int z =0; z<5; z++){
                System.out.println(Sequence[j][z] + " ");
            }
            System.out.println(" ------ ");

        }
    }

}