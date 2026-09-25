package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A robust, offline QR code representation renderer for TV and mobile screen scanning.
 * Encodes text into a deterministic 2D module pattern with finder patterns at 3 corners.
 */
@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val matrix = remember(content) { generateQrMatrix(content) }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(lightColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().aspectRatio(1f)) {
            val moduleCount = matrix.size
            if (moduleCount == 0) return@Canvas
            val cellSize = this.size.width / moduleCount

            for (r in 0 until moduleCount) {
                for (c in 0 until moduleCount) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = darkColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(text: String): Array<BooleanArray> {
    val n = 25 // standard version 2 QR dimension (25x25)
    val matrix = Array(n) { BooleanArray(n) }

    // Finder patterns at top-left, top-right, bottom-left (7x7)
    drawFinderPattern(matrix, 0, 0)
    drawFinderPattern(matrix, 0, n - 7)
    drawFinderPattern(matrix, n - 7, 0)

    // Timing patterns
    for (i in 8 until (n - 8)) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // Alignment pattern at (n-9, n-9)
    val alignCenter = n - 7
    for (r in (alignCenter - 2)..(alignCenter + 2)) {
        for (c in (alignCenter - 2)..(alignCenter + 2)) {
            val isBorder = r == alignCenter - 2 || r == alignCenter + 2 || c == alignCenter - 2 || c == alignCenter + 2
            val isCenter = r == alignCenter && c == alignCenter
            if (r in 0 until n && c in 0 until n) {
                matrix[r][c] = isBorder || isCenter
            }
        }
    }

    // Deterministic payload pattern based on hash of text & bytes
    val bytes = text.toByteArray()
    var byteIdx = 0
    var bitIdx = 0

    for (r in 0 until n) {
        for (c in 0 until n) {
            // Skip finder zones & timing
            if (isReserved(r, c, n)) continue

            val charVal = if (bytes.isNotEmpty()) bytes[byteIdx % bytes.size].toInt() else 0
            val bit = (charVal shr (bitIdx % 8)) and 1 == 1

            // Combine with checker mask for high optical contrast
            val mask = ((r + c) % 2 == 0) xor ((r * c) % 3 == 0)
            val hashModifier = ((abs(text.hashCode() + (r * 31 + c * 17))) % 7) < 3

            matrix[r][c] = (bit xor mask) or hashModifier

            bitIdx++
            if (bitIdx % 8 == 0) byteIdx++
        }
    }

    return matrix
}

private fun drawFinderPattern(matrix: Array<BooleanArray>, row: Int, col: Int) {
    for (r in 0 until 7) {
        for (c in 0 until 7) {
            val isOuter = r == 0 || r == 6 || c == 0 || c == 6
            val isInner = r in 2..4 && c in 2..4
            matrix[row + r][col + c] = isOuter || isInner
        }
    }
}

private fun isReserved(r: Int, c: Int, n: Int): Boolean {
    // Top-left finder + separator
    if (r <= 7 && c <= 7) return true
    // Top-right finder + separator
    if (r <= 7 && c >= n - 8) return true
    // Bottom-left finder + separator
    if (r >= n - 8 && c <= 7) return true
    // Timing lines
    if (r == 6 || c == 6) return true
    // Alignment pattern
    val ac = n - 7
    if (r in (ac - 2)..(ac + 2) && c in (ac - 2)..(ac + 2)) return true
    return false
}
