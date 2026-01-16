<template>
  <div class="trade-container">
    <button @click="goBack" class="back-btn">&larr; 뒤로 가기</button>

    <div v-if="stock.name" class="stock-header">
      <h2>{{ stock.name }} ({{ stock.code }})</h2>
      <div class="current-price">
        <span v-if="priceLoading">현재가: 확인 중...</span>
        <span v-else-if="stock.price">현재가: {{ stock.price }}원</span>
        <span v-else class="price-error">현재가를 불러올 수 없습니다.</span>
      </div>
    </div>

    <div v-if="initialError" class="error">{{ initialError }}</div>

    <div v-if="stock.name" class="order-section">
      <div class="order-type">
        <button :class="{ active: orderType === 'buy' }" @click="orderType = 'buy'">매수</button>
        <button :class="{ active: orderType === 'sell' }" @click="orderType = 'sell'">매도</button>
      </div>

      <div class="price-type">
        <button :class="{ active: priceType === 'market' }" @click="setPriceType('market')">시장가</button>
        <button :class="{ active: priceType === 'limit' }" @click="setPriceType('limit')">지정가</button>
      </div>

      <div class="order-form">
        <div class="form-group">
          <label for="quantity">수량:</label>
          <input type="number" id="quantity" v-model.number="quantity" min="1">
        </div>
        <div class="form-group">
          <label for="price">가격:</label>
          <input 
            type="number" 
            id="price" 
            v-model.number="orderPrice" 
            :disabled="priceType === 'market'"
            step="100"
          >
        </div>
        <div class="order-total">
          <span>주문 총액: {{ orderTotal }}원</span>
        </div>
        <button class="order-btn" @click="placeOrder" :disabled="priceLoading || !stock.price">
          {{ orderType === 'buy' ? '매수 주문' : '매도 주문' }}
        </button>
      </div>
    </div>
    
    <div v-if="orderResult" class="order-result" :class="{ 'success': orderResult.success, 'failure': !orderResult.success }">
      <p>{{ orderResult.message }}</p>
    </div>
  </div>
</template>

<script>
export default {
  props: ['stockCode'],
  data() {
    return {
      stock: {
        name: null,
        code: null,
        price: null,
      },
      orderType: 'buy',
      priceType: 'market',
      quantity: 1,
      orderPrice: 0,
      orderResult: null,
      priceLoading: false,
      initialError: null, 
    };
  },
  computed: {
    orderTotal() {
      const price = this.priceType === 'market' ? (this.stock.price || 0) : this.orderPrice;
      return this.quantity * price;
    },
  },
  methods: {
    goBack() {
      this.$router.back();
    },
    setPriceType(type) {
      this.priceType = type;
      if (type === 'market' && this.stock.price) {
        this.orderPrice = this.stock.price;
      }
    },
    async fetchStockPrice() {
      this.priceLoading = true;
      try {
        const response = await fetch(`/api/stock/details/${this.stock.code}`);
        if (!response.ok) {
            throw new Error('가격을 불러오지 못했습니다.');
        }
        const data = await response.json();
        if (!data || !data.price) {
            throw new Error('수신된 가격 정보가 유효하지 않습니다.');
        }
        this.stock.price = data.price;
        if (this.priceType === 'market') {
            this.orderPrice = data.price;
        }
      } catch (e) {
        console.error('API 호출 에러:', e.message);
      } finally {
        this.priceLoading = false;
      }
    },
    async getMyStocks() {
      try {
        const response = await fetch('/api/my-stocks');
        if (!response.ok) {
          throw new Error('보유 주식 정보를 가져오지 못했습니다.');
        }
        const data = await response.json();
        console.log('Refreshed user stock data:', data);
      } catch (error) {
        console.error('Error refreshing user stock data:', error);
      }
    },
    async placeOrder() {
      if (this.quantity <= 0) {
        this.orderResult = {
          success: false,
          message: '주문 수량은 1 이상이어야 합니다.',
        };
        return;
      }

      this.orderResult = null;
      
      const orderDetails = {
          stockCode: this.stock.code,
          quantity: this.quantity,
      };

      try {
          const endpoint = this.orderType === 'buy' ? '/api/stock/buy' : '/api/stock/sell';
          const response = await fetch(endpoint, {
              method: 'POST',
              headers: {
                  'Content-Type': 'application/json',
              },
              body: JSON.stringify(orderDetails),
          });

          const result = await response.json();

          if (!response.ok) {
              throw new Error(result.error || `주문 처리 중 서버에서 오류가 발생했습니다.`);
          }
          
          if(result.rt_cd !== '0') {
            this.orderResult = {
              success: false,
              message: `주문 실패: ${result.msg1}`
            };
          } else {
            this.orderResult = {
              success: true,
              message: `(${this.orderType === 'buy' ? '매수' : '매도'}) 주문이 성공적으로 접수되었습니다.`,
            };
            this.getMyStocks();
            this.goBack();
          }

      } catch (error) {
          console.error('Error placing order:', error);
          this.orderResult = {
              success: false,
              message: `주문 처리 중 오류가 발생했습니다: ${error.message}`,
          };
      }
    },
  },
  created() {
    const name = this.$route.query.name;
    if (!this.stockCode || !name) {
        this.initialError = "종목 정보가 올바르지 않습니다.";
        return;
    }
    this.stock.name = name;
    this.stock.code = this.stockCode;
    this.fetchStockPrice();
  },
  watch: {
    'stock.price'(newPrice) {
      if (this.priceType === 'market' && newPrice) {
        this.orderPrice = newPrice;
      }
    }
  },
};
</script>

<style scoped>
.back-btn {
  background: none;
  border: none;
  font-size: 1rem;
  color: #007bff;
  cursor: pointer;
  margin-bottom: 1rem;
  padding: 0;
  align-self: flex-start;
}
.trade-container {
  max-width: 400px;
  margin: 2rem auto;
  padding: 2rem;
  border: 1px solid #ccc;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}
.stock-header {
    margin-bottom: 1.5rem;
}
.current-price {
  font-size: 1.2rem;
  font-weight: bold;
}
.price-error {
    color: #d32f2f;
    font-size: 1rem;
}
.order-section {
  border-top: 1px solid #eee;
  padding-top: 1.5rem;
}
.order-type, .price-type {
  display: flex;
  margin-bottom: 1rem;
}
.order-type button, .price-type button {
  flex-grow: 1;
  padding: 0.8rem;
  border: 1px solid #ccc;
  background-color: #f9f9f9;
  cursor: pointer;
}
.order-type button.active, .price-type button.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}
.form-group {
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
}
.form-group label {
  width: 80px;
}
.form-group input {
  flex-grow: 1;
  padding: 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
}
.form-group input:disabled {
  background-color: #e9ecef;
  color: #495057;
}
.order-total {
  margin-bottom: 1rem;
  text-align: right;
  font-weight: bold;
}
.order-btn {
  width: 100%;
  padding: 0.8rem;
  background-color: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.order-btn:disabled {
    background-color: #ccc;
    cursor: not-allowed;
}
.order-result {
  margin-top: 1.5rem;
  padding: 1rem;
  border-radius: 4px;
}
.order-result.success {
  background-color: #e9f7ef;
  color: #155724;
}
.order-result.failure {
  background-color: #fce8e6;
  color: #c5221f;
}

.error {
    padding: 2em;
    text-align: center;
    color: #d32f2f;
    background-color: #ffcdd2;
}
</style>
