package me.happyman.utils.Music;

public enum Note
{
    F_SHARP_OCTAVE_ONE(0.5f, "F♯/G♭", false),
    G_OCTAVE_ONE(0.529732f, "G", false),
    G_SHARP_OCTAVE_ONE(0.561231f, "G♯/A♭", false),
    A_OCTAVE_ONE(0.594604f, "A", false),
    A_SHARP_OCTAVE_ONE(0.629961f, "A♯/B♭", false),
    B_OCTAVE_ONE(0.667420f, "B", false),
    C_OCTAVE_ONE(0.707107f, "C", false),
    C_SHARP_OCTAVE_ONE(0.749154f, "C♯/D♭", false),
    D_OCTAVE_ONE(0.793701f, "D", false),
    D_SHARP_OCTAVE_ONE(0.840896f, "D♯/E♭", false),
    E_OCTAVE_ONE(0.890899f, "E", false),
    F_OCTAVE_ONE(0.943874f, "F", true),
    F_SHARP_OCTAVE_TWO(1f, "F♯/G♭", false),
    G_OCTAVE_TWO(1.05946f, "G", false),
    G_SHARP_OCTAVE_TWO(1.12246f, "G♯/A♭", false),
    A_OCTAVE_TWO(1.18921f, "A", false),
    A_SHARP_OCTAVE_TWO(1.25992f, "A♯/B♭", false),
    B_OCTAVE_TWO(1.33484f, "B", false),
    C_OCTAVE_TWO(1.41421f, "C", false),
    C_SHARP_OCTAVE_TWO(1.49831f, "C♯/D♭", false),
    D_OCTAVE_TWO(1.58740f, "D", false),
    D_SHARP_OCTAVE_TWO(1.68179f, "D♯/E♭", false),
    E_OCTAVE_TWO(1.78180f, "E", false),
    F_OCTAVE_TWO(1.88775f, "F", true),
    F_SHARP_OCTAVE_THREE(2f, "F♯/G♭", false);

    private final float pitch;
    private final String name;
    private final boolean isAtEndOfOctave;

    Note(float pitch, String name, boolean isAtEndOfOctave)
    {
        this.pitch = pitch;
        this.name = name;
        this.isAtEndOfOctave = isAtEndOfOctave;
    }

    public float getPitch()
    {
        return pitch;
    }

    public String getName()
    {
        return name;
    }
    
    public boolean isAtEndOfOctave()
    {
        return isAtEndOfOctave;
    }
}
