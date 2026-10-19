package com.camara.app

import android.app.Application

/**
 * Application entry point. Kept intentionally light — heavy work (CameraX binding,
 * GL setup) happens per-screen so resources are released when the user leaves.
 */
class CamaraApp : Application()
