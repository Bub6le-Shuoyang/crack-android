package com.example.monitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.monitor.network.DetectionResult
import com.example.monitor.utils.LabelUtils

class BoundingBoxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        textSize = 40f
        style = Paint.Style.FILL
    }

    private var results: List<DetectionResult> = emptyList()
    private var imageWidth: Float = 1f
    private var imageHeight: Float = 1f
    private var viewWidth: Float = 1f
    private var viewHeight: Float = 1f
    
    // Scale and offset parameters for fitCenter
    private var scale: Float = 1f
    private var dx: Float = 0f
    private var dy: Float = 0f

    fun setResults(results: List<DetectionResult>, imageWidth: Float, imageHeight: Float) {
        this.results = results
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewWidth = w.toFloat()
        viewHeight = h.toFloat()
        calculateTransform()
    }
    
    private fun calculateTransform() {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) return
        
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        
        scale = minOf(scaleX, scaleY)
        
        dx = (viewWidth - imageWidth * scale) / 2f
        dy = (viewHeight - imageHeight * scale) / 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        calculateTransform()

        for (result in results) {
            // Calculate coordinates (API might return center x,y or top-left. Assuming center for YOLO format typically, but let's assume standard top-left if not specified. Wait, standard YOLO output is often center x, center y, width, height. Let's look at the example: x: 100.5, y: 200.3, width: 50.0, height: 80.0. Let's assume it's top-left x, y.)
            // If it's center:
            // val left = result.x - result.width / 2
            // val top = result.y - result.height / 2
            // For now let's assume it's top-left.
            
            // Actually, typical YOLO format is center x, center y, width, height. Let's handle it as top-left for safety, if it looks wrong we can adjust. Let's check API doc: x, y, width, height. Usually it's top-left in web APIs. Let's use top-left.
            val left = result.x.toFloat()
            val top = result.y.toFloat()
            val right = left + result.width.toFloat()
            val bottom = top + result.height.toFloat()

            // Scale to view
            val scaledLeft = left * scale + dx
            val scaledTop = top * scale + dy
            val scaledRight = right * scale + dx
            val scaledBottom = bottom * scale + dy

            val rect = RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)
            
            val (mappedLabel, mappedColor) = LabelUtils.getMappedLabelAndColor(result.label)
            paint.color = mappedColor
            
            canvas.drawRect(rect, paint)
            textPaint.color = mappedColor
            
            canvas.drawRect(rect, paint)
            
            val labelText = "$mappedLabel ${(result.score * 100).toInt()}%"
            canvas.drawText(labelText, scaledLeft, scaledTop - 10f, textPaint)
        }
    }
}
