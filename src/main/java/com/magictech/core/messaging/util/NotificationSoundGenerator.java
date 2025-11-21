package com.magictech.core.messaging.util;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * Utility to generate a simple notification sound WAV file.
 * Run this once to create the notification.wav file.
 */
public class NotificationSoundGenerator {

    public static void main(String[] args) {
        try {
            String outputPath = "src/main/resources/sounds/notification.wav";
            generateNotificationSound(outputPath);
            System.out.println("Notification sound generated successfully at: " + outputPath);
        } catch (Exception e) {
            System.err.println("Error generating notification sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Generate a pleasant notification beep sound.
     * Creates a two-tone beep (C and E notes) with fade in/out.
     */
    public static void generateNotificationSound(String outputPath) throws IOException, LineUnavailableException {
        // Audio parameters
        float sampleRate = 44100;
        int duration = 400; // milliseconds
        int sampleCount = (int) (sampleRate * duration / 1000);

        // Create audio buffer
        byte[] buffer = new byte[sampleCount * 2]; // 16-bit = 2 bytes per sample

        // Generate two-tone pleasant beep
        for (int i = 0; i < sampleCount; i++) {
            // First tone: 800 Hz for first half
            // Second tone: 1000 Hz for second half
            double frequency = (i < sampleCount / 2) ? 800.0 : 1000.0;

            // Calculate sample value
            double angle = 2.0 * Math.PI * i * frequency / sampleRate;
            double sample = Math.sin(angle);

            // Apply envelope (fade in/out) for smooth sound
            double envelope = 1.0;
            int fadeLength = sampleCount / 10; // 10% fade

            if (i < fadeLength) {
                // Fade in
                envelope = (double) i / fadeLength;
            } else if (i > sampleCount - fadeLength) {
                // Fade out
                envelope = (double) (sampleCount - i) / fadeLength;
            }

            sample *= envelope;

            // Convert to 16-bit PCM
            short pcmSample = (short) (sample * 32767 * 0.5); // 50% volume

            // Write to buffer (little-endian)
            buffer[i * 2] = (byte) (pcmSample & 0xff);
            buffer[i * 2 + 1] = (byte) ((pcmSample >> 8) & 0xff);
        }

        // Write to WAV file
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        AudioInputStream audioInputStream = new AudioInputStream(bais, format, buffer.length / 2);

        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);

        audioInputStream.close();
        bais.close();
    }
}
