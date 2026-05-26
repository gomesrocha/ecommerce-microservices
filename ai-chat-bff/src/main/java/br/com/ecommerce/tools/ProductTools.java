package br.com.ecommerce.tools;

import br.com.ecommerce.client.ProductClient;
import br.com.ecommerce.dto.ProductResponse;
import dev.langchain4j.agent.tool.Tool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ProductTools {
    private static final Logger LOG = Logger.getLogger(ProductTools.class);


    @Inject
    @RestClient
    ProductClient productClient;

    @Tool("Lista os produtos disponíveis no ecommerce")
    public List<ProductResponse> listarProdutos() {
        return productClient.listProducts();
    }

    @Tool("Busca os detalhes de um produto pelo ID")
    public ProductResponse buscarProdutoPorId(Long productId) {
        return productClient.findById(productId);
    }
}