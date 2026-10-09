"""Genera un WAV sintético + el FloatArray que Kotlin debe producir."""
from pathlib import Path
import numpy as np
import torch
import torchaudio

SR = 16000
CLIP = 16000
N_FFT = 400
HOP = 160
N_MELS = 64
F_MIN = 0.0
F_MAX = 8000.0
MEAN = -33.879398345947266
STD = 23.381967544555664

# 1) Generar audio de prueba: mezcla de 3 senoidales + ruido blanco suave
rng = np.random.default_rng(42)
t = np.arange(CLIP) / SR
signal = (
    0.5 * np.sin(2 * np.pi * 440 * t) +
    0.3 * np.sin(2 * np.pi * 880 * t) +
    0.2 * np.sin(2 * np.pi * 1500 * t) +
    0.05 * rng.standard_normal(CLIP)
)
signal = np.clip(signal, -1.0, 1.0)

# Guardar como WAV 16-bit mono
pcm16 = (signal * 32767).astype(np.int16)
wav_path = Path("test_audio.wav")
import wave
with wave.open(str(wav_path), "wb") as w:
    w.setnchannels(1)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(pcm16.tobytes())
print(f"WAV guardado: {wav_path.resolve()} ({CLIP} muestras)")

# 2) Calcular el mel-espectrograma normalizado como en entrenamiento
waveform = torch.from_numpy(signal.astype(np.float32)).unsqueeze(0)  # (1, T)
mel_transform = torchaudio.transforms.MelSpectrogram(
    sample_rate=SR, n_mels=N_MELS, n_fft=N_FFT, win_length=N_FFT,
    hop_length=HOP, f_min=F_MIN, f_max=F_MAX, power=2.0,
    center=True, pad_mode="reflect", mel_scale="htk", norm=None,
)
mel = mel_transform(waveform)                                  # (1, 64, 101)
db = torchaudio.functional.amplitude_to_DB(mel, multiplier=10.0, amin=1e-10, db_multiplier=0.0)
spec = ((db - MEAN) / STD).squeeze(0).numpy().astype(np.float32)  # (64, 101)

print(f"Shape esperado: {spec.shape}, dtype: {spec.dtype}")
print(f"Primeros 5 valores: {spec.flatten()[:5]}")
print(f"Media: {spec.mean():.6f}, Std: {spec.std():.6f}, Min: {spec.min():.6f}, Max: {spec.max():.6f}")

# 3) Guardar el .npy para comparar en Kotlin
np.save("test_spec_expected.npy", spec.flatten())
print(f"Esperado guardado en: test_spec_expected.npy ({spec.size} elementos)")

# 4) Exportar PCM int16 crudo (sin header WAV) para Kotlin
raw_pcm_path = Path("test_audio.raw")
pcm16.astype("<i2").tofile(raw_pcm_path)  # little-endian int16
print(f"PCM crudo guardado en: {raw_pcm_path.resolve()} ({pcm16.size} muestras)")

# 5) Exportar espectrograma crudo como float32 little-endian para Kotlin
bin_path = Path("test_spec_expected.bin")
spec.flatten().astype("<f4").tofile(bin_path)
print(f"Espectrograma guardado en: {bin_path.resolve()} ({spec.size} floats)")