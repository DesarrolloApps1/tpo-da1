## Historia

Cierra #<!-- número del issue, ej. US-01 es #3 -->. Si es un PR parcial, poné "Parte de #N" en vez de "Cierra".

## Qué cambia

<!-- Qué hace este PR, en 2 o 3 líneas. -->

## Cómo se probó

- [ ] Compila y corre en Android Studio
- [ ] Tests nuevos o actualizados
- [ ] Probado en emulador (API: ) o teléfono

## Uso de IA (consigna, sección 8)

Si usaste IA para este PR, respondé antes de pedir revisión:

1. **¿Qué problema resuelve este código?**
2. **¿Qué capa o responsabilidad modifica?** (ui, presentation, domain, data, platform, di)
3. **¿Qué dependencias agrega?**
4. **¿Existe una alternativa más simple?**
5. **¿Cómo comprobamos que funciona?**
6. **¿Qué pasaría si falla?**

## Checklist

- [ ] El dominio no importa nada de `android.*`
- [ ] Los ViewModels no usan Room ni Retrofit directamente
- [ ] Commits chicos con prefijo (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`)
- [ ] Puedo explicar todo el código de este PR sin leerlo
