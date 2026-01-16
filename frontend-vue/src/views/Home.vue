<template>
  <div id="home">
    <div class="content-wrapper">
      <div class="portfolio-card">
        <h1>나의 주식 포트폴리오</h1>

        <!-- Account Summary Section -->
        <div v-if="accountSummary" class="account-summary">
          <div class="summary-item">
            <span class="label">계좌번호</span>
            <span class="value">{{ accountNumber }}</span>
          </div>
          <div class="summary-item">
            <span class="label">예수금</span>
            <span class="value">{{ formatCurrency(accountSummary.dnca_tot_amt) }}원</span>
          </div>
        </div>

        <p v-if="error" class="error">{{ error }}</p>

        <div v-if="loading" class="loading-indicator">
          <p>데이터를 불러오는 중...</p>
        </div>

        <div v-if="!loading && stocks.length > 0" class="stock-table-container">
          <h2>보유 주식</h2>
          <table>
            <thead>
              <tr>
                <th>상품명</th>
                <th>보유수량</th>
                <th>매수평균단가</th>
                <th>평가금액</th>
                <th>수익률 (%)</th>
                <th>수익금</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="stock in stocks" :key="stock.pdno" @click="goToTradePage(stock)" class="clickable-row">
                <td>{{ stock.prdt_name }} <span class="stock-code">({{ stock.pdno }})</span></td>
                <td class="text-right">{{ stock.hldg_qty }}</td>
                <td class="text-right">{{ formatCurrency(stock.pchs_avg_pric, true) }}원</td>
                <td class="text-right">{{ formatCurrency(stock.evlu_amt) }}원</td>
                <td :class="getRateClass(stock.fltt_rt)" class="text-right">{{ stock.fltt_rt }}%</td>
                <td :class="getRateClass(stock.fltt_rt)" class="text-right">
                  {{ formatCurrency(stock.evlu_pfls_amt) }}원
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="!loading && stocks.length === 0 && !error && loaded" class="no-stocks">보유 주식이 없습니다.</p>
      </div>

      <!-- Top Traded Stocks Section -->
      <div class="hot-stocks-card">
        <h2>거래량 상위 추천 주식</h2>
        <button @click="fetchTopTradedStocks" :disabled="topTradedLoading">
          {{ topTradedLoading ? '불러오는 중...' : '추천 주식 보기' }}
        </button>

        <div v-if="topTradedLoading" class="loading-indicator">
          <p>데이터를 불러오고 있습니다...</p>
        </div>

        <p v-if="topTradedError" class="error">{{ topTradedError }}</p>

        <div v-if="topTradedStocks.length > 0" class="hot-stocks-list">
          <h3>추천 목록</h3>
          <ul>
            <li v-for="stock in topTradedStocks" :key="stock.mksc_shrn_iscd" @click="goToTradePage(stock)" class="clickable-row">
              <div class="stock-info">
                <span class="stock-name">{{ stock.hts_kor_isnm }}</span>
                <span class="stock-code">({{ stock.mksc_shrn_iscd }})</span>
              </div>
            </li>
          </ul>
        </div>
        <p v-if="!topTradedLoading && topTradedStocks.length === 0 && !topTradedError && topTradedLoaded" class="no-stocks">추천 주식 정보가 없습니다.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Home',
  data() {
    return {
      stocks: [],
      accountSummary: null,
      accountNumber: '',
      error: null,
      loaded: false,
      loading: false,
      topTradedStocks: [],
      topTradedLoading: false,
      topTradedError: null,
      topTradedLoaded: false,
    };
  },
  created() {
    this.fetchMyStocks();
  },
  methods: {
    goToTradePage(stock) {
      const stockCode = stock.pdno || stock.mksc_shrn_iscd;
      const stockName = stock.prdt_name || stock.hts_kor_isnm;

      if (!stockCode) {
        console.error("Stock or stock code is missing!");
        return;
      }
      this.$router.push({ 
        path: `/trade/${stockCode}`,
        query: { name: stockName }
      });
    },
    async fetchMyStocks() {
      this.error = null;
      this.loaded = false;
      this.loading = true;
      try {
        const response = await fetch('/api/stock/my-stocks');
        if (!response.ok) {
          const errorData = await response.json().catch(() => ({ error: '서버 응답을 파싱할 수 없습니다.' }));
          throw new Error(errorData.error || `HTTP error! status: ${response.status}`);
        }
        const data = await response.json();
        this.stocks = data.stocks || [];
        this.accountSummary = data.accountSummary || null;
        this.accountNumber = data.accountNumber || '';
        
      } catch (e) {
        console.error('API 호출 에러:', e);
        this.error = e.message || '데이터를 불러오는 데 실패했습니다.';
        this.stocks = [];
        this.accountSummary = null;
      } finally {
        this.loaded = true;
        this.loading = false;
      }
    },
    async fetchTopTradedStocks() {
      this.topTradedError = null;
      this.topTradedLoading = true;
      this.topTradedLoaded = false;
      this.topTradedStocks = [];

      try {
        const response = await fetch('/api/stock-recommendation/top-traded');
        if (!response.ok) {
          throw new Error('추천 주식 정보를 가져오는 데 실패했습니다.');
        }
        const data = await response.json();
        this.topTradedStocks = data.output || [];
      } catch (e) {
        console.error('Top Traded Stocks 에러:', e);
        this.topTradedError = e.message || '오류가 발생했습니다.';
      } finally {
        this.topTradedLoading = false;
        this.topTradedLoaded = true;
      }
    },
    formatCurrency(value, shouldRound = false) {
      if (value === null || value === undefined) return '0';
      let numValue = parseFloat(value);
      if (isNaN(numValue)) return '0';

      if (shouldRound) {
        numValue = Math.round(numValue);
      }
      
      return new Intl.NumberFormat('ko-KR').format(numValue);
    },
    getRateClass(rate) {
      const rateValue = parseFloat(rate);
      if (rateValue > 0) return 'positive';
      if (rateValue < 0) return 'negative';
      return 'zero';
    },
  },
};
</script>

<style scoped>
#home {
  width: 100%;
  max-width: 2800px;
  margin: 0 auto;
}
.content-wrapper {
  display: flex;
  gap: 2em;
  align-items: flex-start;
}
.clickable-row {
    cursor: pointer;
}
.stock-code {
  color: #888;
  font-size: 0.9em;
  margin-left: 0.5em;
}
.portfolio-card {
  flex: 3; /* Make portfolio card wider */
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  padding: 2.5em;
  text-align: center;
  transition: all 0.3s ease;
  margin-bottom: 0;
  min-width: 0;
}

.hot-stocks-card {
  flex: 1;
  background-color: #ffffff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
  padding: 2.5em;
  text-align: center;
  transition: all 0.3s ease;
  margin-bottom: 0;
  min-width: 0;
}

.portfolio-card:hover, .hot-stocks-card:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

h1 {
  color: #333;
  margin-bottom: 0.5em;
  font-weight: 600;
}

h2 {
    color: #444;
    text-align: center;
    border-bottom: 2px solid #f0f0f0;
    padding-bottom: 0.5em;
    margin-top: 0;
    margin-bottom: 1.5em;
}

h3 {
  color: #555;
  text-align: left;
  margin-top: 1.5em;
  margin-bottom: 1em;
}

/* Account Summary */
.account-summary {
    background-color: #f9fafb;
    border-radius: 8px;
    padding: 1.5em;
    margin-top: 1.5em;
    margin-bottom: 2em;
    display: flex;
    justify-content: space-around;
    border: 1px solid #e0e0e0;
}

.summary-item {
    text-align: center;
}

.summary-item .label {
    display: block;
    font-size: 14px;
    color: #555;
    margin-bottom: 0.5em;
}

.summary-item .value {
    display: block;
    font-size: 1.5em;
    color: #333;
    font-weight: 600;
}

/* Button Style */
button {
  background: linear-gradient(45deg, #4CAF50, #81C784);
  color: white;
  border: none;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  margin-bottom: 1em;
}

button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(76, 175, 80, 0.4);
}

button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

/* Table Styles */
.stock-table-container {
  overflow-x: auto;
  margin-top: 2em;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1.5em;
  font-size: 15px;
}

th, td {
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

thead th {
  background-color: #f9fafb;
  color: #555;
  font-weight: 600;
  text-align: center;
  white-space: nowrap;
}

tbody tr:hover {
  background-color: #f5f5f5;
}

/* Hot Stocks List */
.hot-stocks-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
}

.hot-stocks-list li {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 1em;
  margin-bottom: 1em;
  border-left: 4px solid #4CAF50;
}

.hot-stocks-list .stock-info {
  display: flex;
  align-items: center;
  margin-bottom: 0.5em;
}

.hot-stocks-list .stock-name {
  font-weight: 600;
  font-size: 1.1em;
}

.hot-stocks-list .summary {
  color: #666;
  font-size: 0.95em;
  line-height: 1.5;
}


/* Text alignment */
.text-right {
  text-align: right;
}

/* Profit/Loss Rate Colors */
.positive {
  color: #d32f2f; /* Red for profit */
  font-weight: 500;
}

.negative {
  color: #1976d2; /* Blue for loss */
  font-weight: 500;
}

.zero {
    color: #333;
}

/* Message Styles */
.error {
  color: #d32f2f;
  background-color: #ffcdd2;
  padding: 1em;
  border-radius: 8px;
  margin-top: 1em;
}

.no-stocks {
  color: #555;
  margin-top: 2em;
  font-size: 1.1em;
}
.loading-indicator {
    padding: 2em;
    text-align: center;
    color: #555;
}
</style>
