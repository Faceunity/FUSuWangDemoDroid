package com.chinanetcenter.streampusherdemo.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

public class OpenglUtil {

    public static final int NO_TEXTURE = -1;
    public static final int NOT_INIT = -1;
    public static final int ON_DRAWN = 1;

    public static final float TEXTURE_NO_ROTATION[] = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,};
    public static final float TEXTURE_ROTATED_90[] = {1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,};
    public static final float TEXTURE_ROTATED_180[] = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,};
    public static final float TEXTURE_ROTATED_270[] = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,};

    public static final float CUBE[] = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f,};

    public static float[] getRotation(final int rotation, final boolean flipHorizontal, final boolean flipVertical) {
        float[] rotatedTex;
        switch (rotation) {
            case 90:
                rotatedTex = TEXTURE_ROTATED_90;
                break;
            case 180:
                rotatedTex = TEXTURE_ROTATED_180;
                break;
            case 270:
                rotatedTex = TEXTURE_ROTATED_270;
                break;
            default:
                rotatedTex = TEXTURE_NO_ROTATION;
                break;
        }
        if (flipHorizontal) {
            rotatedTex = new float[]{flip(rotatedTex[0]), rotatedTex[1], flip(rotatedTex[2]), rotatedTex[3],
                    flip(rotatedTex[4]), rotatedTex[5], flip(rotatedTex[6]), rotatedTex[7],};
        }
        if (flipVertical) {
            rotatedTex = new float[]{rotatedTex[0], flip(rotatedTex[1]), rotatedTex[2], flip(rotatedTex[3]),
                    rotatedTex[4], flip(rotatedTex[5]), rotatedTex[6], flip(rotatedTex[7]),};
        }
        return rotatedTex;
    }

    private static float flip(final float i) {
        if (i == 0.0f) {
            return 1.0f;
        }
        return 0.0f;
    }

    public static int[] initTextureID(int width, int height) {
        int[] mTextureOutID = new int[1];
        GLES20.glGenTextures(1, mTextureOutID, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureOutID[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE, null);
        return mTextureOutID;
    }

    public static int loadProgram(final String strVSource, final String strFSource) {
        int iVShader;
        int iFShader;
        int iProgId;
        int[] link = new int[1];
        iVShader = loadShader(strVSource, GLES20.GL_VERTEX_SHADER);
        if (iVShader == 0) {
            Log.d("Load Program", "Vertex Shader Failed");
            return 0;
        }
        iFShader = loadShader(strFSource, GLES20.GL_FRAGMENT_SHADER);
        if (iFShader == 0) {
            Log.d("Load Program", "Fragment Shader Failed");
            return 0;
        }

        iProgId = GLES20.glCreateProgram();

        GLES20.glAttachShader(iProgId, iVShader);
        GLES20.glAttachShader(iProgId, iFShader);

        GLES20.glLinkProgram(iProgId);

        GLES20.glGetProgramiv(iProgId, GLES20.GL_LINK_STATUS, link, 0);
        if (link[0] <= 0) {
            Log.d("Load Program", "Linking Failed");
            return 0;
        }
        GLES20.glDeleteShader(iVShader);
        GLES20.glDeleteShader(iFShader);
        return iProgId;
    }

    private static int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    public static int[] loadTexture(final Bitmap img, final int usedTexId, boolean recyled) {
        int textures[] = new int[1];
        if (usedTexId == NO_TEXTURE) {
            GLES20.glGenTextures(1, textures, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, img, 0);
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, usedTexId);
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, img);
            textures[0] = usedTexId;
        }
        if (recyled)
            img.recycle();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textures;
    }

    // Assert that no OpenGL ES 2.0 error has been raised.
    public static void checkNoGLES2Error(String msg) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            throw new RuntimeException(msg + ": GLES20 error: " + error);
        }
    }

    /**
     * Returns texture matrix that will have the effect of rotating the frame
     * |rotationDegree| clockwise when rendered.
     */
    public static float[] rotateTextureMatrix(float[] textureMatrix, float rotationDegree) {
        final float[] rotationMatrix = new float[16];
        Matrix.setRotateM(rotationMatrix, 0, rotationDegree, 0, 0, 1);
        adjustOrigin(rotationMatrix);
        return multiplyMatrices(textureMatrix, rotationMatrix);
    }

    /**
     * Move |matrix| transformation origin to (0.5, 0.5). This is the origin for
     * texture coordinates that are in the range 0 to 1.
     */
    private static void adjustOrigin(float[] matrix) {
        // Note that OpenGL is using column-major order.
        // Pre translate with -0.5 to move coordinates to range [-0.5, 0.5].
        matrix[12] -= 0.5f * (matrix[0] + matrix[4]);
        matrix[13] -= 0.5f * (matrix[1] + matrix[5]);
        // Post translate with 0.5 to move coordinates to range [0, 1].
        matrix[12] += 0.5f;
        matrix[13] += 0.5f;
    }

    /**
     * Returns new matrix with the result of a * b.
     */
    public static float[] multiplyMatrices(float[] a, float[] b) {
        final float[] resultMatrix = new float[16];
        Matrix.multiplyMM(resultMatrix, 0, a, 0, b, 0);
        return resultMatrix;
    }

}
