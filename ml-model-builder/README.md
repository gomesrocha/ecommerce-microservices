# ML Model Builder

Projeto responsável por treinar modelos de Machine Learning com Tribuo para os microsserviços do ecommerce.

## Objetivo

Separar o treinamento dos modelos dos microsserviços de runtime.

Os microsserviços não devem treinar modelos no startup. Eles devem apenas carregar modelos prontos.

## Modelos gerados

```text
models/delivery/delivery-tribuo-v1.model
models/fraud/fraud-tribuo-v1.model
```
