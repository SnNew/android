package com.example.tiendasuplementacion.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.itextpdf.layout.properties.HorizontalAlignment
import com.example.tiendasuplementacion.model.UserOrder
import com.example.tiendasuplementacion.model.OrderProduct
import java.io.File
import java.text.NumberFormat
import java.util.*

object PdfGenerator {
    fun generateInvoicePdf(context: Context, order: UserOrder): String {
        try {
            // Crear el directorio si no existe
            val directory = File(context.getExternalFilesDir(null), "facturas")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Crear el archivo PDF
            val fileName = "factura_${order.order_id}.pdf"
            val file = File(directory, fileName)

            // Configurar el documento PDF
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)

            // Formato para moneda
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))

            // Encabezado
            val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
                .setWidth(UnitValue.createPercentValue(100f))

            // Logo o Nombre de la tienda
            headerTable.addCell(
                Cell().add(
                    Paragraph("Tienda Suplementación")
                        .setFontSize(24f)
                        .setBold()
                ).setBorder(null)
            )

            // Información de la factura
            headerTable.addCell(
                Cell().add(
                    Paragraph("FACTURA")
                        .setFontSize(24f)
                        .setBold()
                        .setTextAlignment(TextAlignment.RIGHT)
                ).add(
                    Paragraph("No. ${order.order_id}")
                        .setFontSize(14f)
                        .setTextAlignment(TextAlignment.RIGHT)
                ).add(
                    Paragraph("Fecha: ${order.date_order}")
                        .setFontSize(12f)
                        .setTextAlignment(TextAlignment.RIGHT)
                ).setBorder(null)
            )

            document.add(headerTable)
            document.add(Paragraph("").setHeight(20f))

            // Información del cliente
            val clienteTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setBackgroundColor(DeviceRgb(240, 240, 240))
                .setPadding(10f)

            order.additionalInfoPayment?.let { paymentInfo ->
                clienteTable.addCell(
                    Cell().add(
                        Paragraph("INFORMACIÓN DEL CLIENTE")
                            .setBold()
                            .setFontSize(14f)
                    ).setBorder(null)
                )
                
                val addressInfo = listOfNotNull(
                    paymentInfo.addressLine1,
                    listOfNotNull(
                        paymentInfo.city,
                        paymentInfo.stateOrProvince,
                        paymentInfo.postalCode
                    ).joinToString(", "),
                    paymentInfo.country
                ).joinToString("\n")

                clienteTable.addCell(
                    Cell().add(Paragraph(addressInfo).setFontSize(12f))
                        .setBorder(null)
                )
            }

            document.add(clienteTable)
            document.add(Paragraph("").setHeight(20f))

            // Estado del pedido
            document.add(
                Paragraph("Estado del pedido: ${order.status.name}")
                    .setFontSize(12f)
                    .setBold()
            )
            document.add(Paragraph("").setHeight(10f))

            // Tabla de productos
            val table = Table(UnitValue.createPercentArray(floatArrayOf(40f, 15f, 15f, 15f, 15f)))
                .setWidth(UnitValue.createPercentValue(100f))

            // Encabezados de la tabla
            listOf("Producto", "Cantidad", "Precio Unit.", "Subtotal", "IVA").forEach { header ->
                table.addHeaderCell(
                    Cell().add(
                        Paragraph(header)
                            .setBold()
                            .setTextAlignment(TextAlignment.CENTER)
                    ).setBackgroundColor(DeviceRgb(240, 240, 240))
                )
            }

            // Productos
            var subtotalConIva = 0.0
            var subtotalSinIva = 0.0
            var ivaTotal = 0.0

            // Agrupar productos por nombre y contar cantidades
            val productosAgrupados = order.products
                .groupBy { "${it.id}_${it.name}_${it.price}" } // Agrupar por una clave única
                .map { entry ->
                    ProductoAgrupado(
                        producto = entry.value.first(),
                        cantidad = entry.value.size // Contar cuántas veces aparece el producto
                    )
                }

            // Procesar cada producto agrupado
            productosAgrupados.forEach { productoAgrupado ->
                val precioConIva = productoAgrupado.producto.price
                val precioSinIva = precioConIva / 1.19 // Extraer el precio base (sin IVA)
                
                val subtotalProductoConIva = precioConIva * productoAgrupado.cantidad
                val subtotalProductoSinIva = precioSinIva * productoAgrupado.cantidad
                val ivaProducto = subtotalProductoConIva - subtotalProductoSinIva
                
                subtotalConIva += subtotalProductoConIva
                subtotalSinIva += subtotalProductoSinIva
                ivaTotal += ivaProducto

                table.addCell(Cell().add(Paragraph(productoAgrupado.producto.name)))
                table.addCell(Cell().add(Paragraph(productoAgrupado.cantidad.toString())).setTextAlignment(TextAlignment.CENTER))
                table.addCell(Cell().add(Paragraph(currencyFormat.format(precioSinIva))).setTextAlignment(TextAlignment.RIGHT))
                table.addCell(Cell().add(Paragraph(currencyFormat.format(subtotalProductoConIva))).setTextAlignment(TextAlignment.RIGHT))
                table.addCell(Cell().add(Paragraph(currencyFormat.format(ivaProducto))).setTextAlignment(TextAlignment.RIGHT))
            }

            // Totales
            table.addCell(Cell(1, 3).add(Paragraph("Subtotal (sin IVA):").setBold()).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph(currencyFormat.format(subtotalSinIva)).setBold()).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell().add(Paragraph(currencyFormat.format(ivaTotal)).setBold()).setTextAlignment(TextAlignment.RIGHT))

            table.addCell(Cell(1, 3).add(Paragraph("Total (IVA incluido):").setBold()).setTextAlignment(TextAlignment.RIGHT))
            table.addCell(Cell(1, 2).add(Paragraph(currencyFormat.format(subtotalConIva)).setBold()).setTextAlignment(TextAlignment.RIGHT))

            document.add(table)

            // Información adicional
            document.add(Paragraph("").setHeight(30f))
            document.add(
                Paragraph("INFORMACIÓN ADICIONAL")
                    .setBold()
                    .setFontSize(14f)
            )
            document.add(Paragraph("").setHeight(10f))

            // Términos y condiciones
            document.add(
                Paragraph("""
                    • Los precios mostrados incluyen IVA del 19%
                    • Base gravable: ${currencyFormat.format(subtotalSinIva)}
                    • IVA (19%): ${currencyFormat.format(ivaTotal)}
                    • Total productos: ${order.total_products}
                    • Esta factura sirve como garantía del producto
                    • Para devoluciones conserve este documento
                    • Tiempo límite para devoluciones: 30 días
                """.trimIndent())
                .setFontSize(10f)
            )

            // Pie de página
            document.add(Paragraph("").setHeight(30f))
            document.add(
                Paragraph("¡Gracias por tu compra!")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(14f)
                    .setBold()
            )
            document.add(
                Paragraph("www.tiendasuplementacion.com")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(10f)
                    .setItalic()
            )

            document.close()
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}

private data class ProductoAgrupado(
    val producto: OrderProduct,
    val cantidad: Int
) 