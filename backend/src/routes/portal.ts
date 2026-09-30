import { FastifyInstance } from 'fastify';
import { resellerStore } from '../db/store';
import { config } from '../config';

export async function portalRoutes(fastify: FastifyInstance) {
  fastify.get('/portal', async (_request, reply) => {
    reply.type('text/html');
    return getPortalHtml();
  });

  fastify.get('/web', async (_request, reply) => {
    reply.type('text/html');
    return getPortalHtml();
  });
}

function getPortalHtml(): string {
  const rates = resellerStore.ratesAndBanking;
  return `<!DOCTYPE html>
<html lang="bn">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Second Number - ভার্চুয়াল নম্বর ও ওটিপি সার্ভিস পোর্টাল</title>
  <style>
    :root {
      --primary: #1A73E8;
      --primary-dark: #1557B0;
      --success: #1E8E3E;
      --bg: #F8F9FA;
      --card-bg: #FFFFFF;
      --text: #202124;
      --subtext: #5F6368;
      --border: #DADCE0;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding-bottom: 60px; }
    header { background: linear-gradient(135deg, #1A73E8, #0D47A1); color: white; padding: 24px 16px; text-align: center; }
    header h1 { font-size: 24px; margin-bottom: 8px; font-weight: 700; }
    header p { font-size: 14px; opacity: 0.9; }
    .container { max-width: 900px; margin: 0 auto; padding: 16px; }
    .card { background: var(--card-bg); border-radius: 12px; border: 1px solid var(--border); padding: 20px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
    .section-title { font-size: 18px; font-weight: 700; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
    .catalog-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; margin-top: 12px; }
    .catalog-card { border: 2px solid var(--border); border-radius: 10px; padding: 16px; cursor: pointer; transition: all 0.2s ease; background: #fff; }
    .catalog-card:hover, .catalog-card.selected { border-color: var(--primary); background: #E8F0FE; }
    .flag { font-size: 24px; margin-right: 8px; }
    .pkg-title { font-weight: 700; font-size: 16px; margin-bottom: 4px; }
    .pkg-badge { display: inline-block; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 700; background: #E8F0FE; color: var(--primary); margin-bottom: 8px; }
    .price-tag { font-size: 20px; font-weight: 800; color: var(--success); margin: 8px 0; }
    .banking-box { background: #F1F3F4; border-radius: 8px; padding: 12px; margin-top: 12px; }
    .bank-row { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid #E0E0E0; font-size: 14px; }
    .bank-row:last-child { border-bottom: none; }
    .copy-btn { background: var(--primary); color: white; border: none; padding: 4px 10px; border-radius: 6px; cursor: pointer; font-size: 12px; }
    .form-group { margin-bottom: 14px; }
    .form-group label { display: block; font-size: 13px; font-weight: 600; margin-bottom: 6px; }
    .form-group input, .form-group select { width: 100%; padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 14px; }
    .btn-submit { width: 100%; background: var(--primary); color: white; border: none; padding: 14px; border-radius: 8px; font-size: 16px; font-weight: 700; cursor: pointer; transition: background 0.2s; }
    .btn-submit:hover { background: var(--primary-dark); }
    .status-alert { display: none; padding: 14px; border-radius: 8px; margin-top: 14px; font-size: 14px; }
    .status-alert.success { display: block; background: #E6F4EA; color: #137333; border: 1px solid #CEEAD6; }
    .status-alert.error { display: block; background: #FCE8E6; color: #C5221F; border: 1px solid #FAD2CF; }
    .tab-nav { display: flex; gap: 8px; margin-bottom: 16px; }
    .tab-btn { flex: 1; padding: 10px; background: #E8EAED; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; text-align: center; }
    .tab-btn.active { background: var(--primary); color: white; }
  </style>
</head>
<body>
  <header>
    <h1>🌐 Second Number সার্ভিস ও রিচার্জ পোর্টাল</h1>
    <p>অনলাইন থেকে সরাসরি ইউএস/ইউকে ভার্চুয়াল নম্বর কিনুন ও ব্যালেন্স রিচার্জ করুন</p>
  </header>

  <div class="container">
    <div class="tab-nav">
      <button class="tab-btn active" onclick="switchTab('buy')">🛒 নম্বর ও প্যাকেজ অর্ডার</button>
      <button class="tab-btn" onclick="switchTab('check')">🔍 ব্যালেন্স ও নম্বর যাচাই</button>
    </div>

    <!-- TAB 1: ORDER / RECHARGE -->
    <div id="tab-buy" class="card">
      <h2 class="section-title">১. আপনার পছন্দের সার্ভিস বা দেশ নির্বাচন করুন</h2>
      <div class="catalog-grid">
        <div class="catalog-card selected" onclick="selectPkg(this, 'US Dedicated Call & SMS', 350, 100, 30)">
          <span class="flag">🇺🇸</span>
          <div class="pkg-title">United States Private Line</div>
          <span class="pkg-badge">কল + এসএমএস</span>
          <p style="font-size: 12px; color: var(--subtext);">পার্সোনাল ইউএস নম্বর দিয়ে ভয়েস কল এবং টেক্সট আদান-প্রদান।</p>
          <div class="price-tag">৳350 BDT <span style="font-size: 12px; color: var(--subtext);">/ ৩০ দিন</span></div>
        </div>

        <div class="catalog-card" onclick="selectPkg(this, 'WhatsApp & Telegram Verification', 180, 30, 10)">
          <span class="flag">🇺🇸</span>
          <div class="pkg-title">WhatsApp / Telegram OTP</div>
          <span class="pkg-badge">সোশ্যাল ভেরিফিকেশন</span>
          <p style="font-size: 12px; color: var(--subtext);">হোয়াটসঅ্যাপ বিজনেস, টেলিগ্রাম, সিগন্যাল খোলার ফ্রেশ নম্বর।</p>
          <div class="price-tag">৳180 BDT <span style="font-size: 12px; color: var(--subtext);">/ ৩০ দিন</span></div>
        </div>

        <div class="catalog-card" onclick="selectPkg(this, '1-Hour Burner OTP Line', 99, 15, 0)">
          <span class="flag">🔥</span>
          <div class="pkg-title">Temporary Burner OTP</div>
          <span class="pkg-badge">১-ঘণ্টা টেম্পোরারি</span>
          <p style="font-size: 12px; color: var(--subtext);">ফেসবুক, গুগল, পেপ্যাল, ওপেন-এআই ভেরিফাই করার তাৎক্ষণিক ওটিপি নম্বর।</p>
          <div class="price-tag">৳99 BDT <span style="font-size: 12px; color: var(--subtext);">/ ১-ঘণ্টা</span></div>
        </div>

        <div class="catalog-card" onclick="selectPkg(this, 'UK London Virtual Line', 450, 80, 25)">
          <span class="flag">🇬🇧</span>
          <div class="pkg-title">United Kingdom (+44) Line</div>
          <span class="pkg-badge">UK কল + এসএমএস</span>
          <p style="font-size: 12px; color: var(--subtext);">লন্ডন ভার্চুয়াল নম্বর দিয়ে আন্তর্জাতিক ক্লায়েন্টদের সাথে যোগাযোগ।</p>
          <div class="price-tag">৳450 BDT <span style="font-size: 12px; color: var(--subtext);">/ ৩০ দিন</span></div>
        </div>
      </div>

      <div style="margin-top: 24px;">
        <h2 class="section-title">২. পেমেন্ট করুন (Send Money)</h2>
        <div class="banking-box">
          <div class="bank-row">
            <span><strong>bKash Personal:</strong> ${rates.bkashNumber}</span>
            <button class="copy-btn" onclick="copyText('${rates.bkashNumber}')">Copy</button>
          </div>
          <div class="bank-row">
            <span><strong>Nagad Personal:</strong> ${rates.nagadNumber}</span>
            <button class="copy-btn" onclick="copyText('${rates.nagadNumber}')">Copy</button>
          </div>
          <div class="bank-row">
            <span><strong>Rocket:</strong> ${rates.rocketNumber}</span>
            <button class="copy-btn" onclick="copyText('${rates.rocketNumber}')">Copy</button>
          </div>
        </div>
      </div>

      <div style="margin-top: 24px;">
        <h2 class="section-title">৩. আপনার পেমেন্টের তথ্য দিন</h2>
        <div class="form-group">
          <label>আপনার নাম:</label>
          <input type="text" id="orderName" placeholder="যেমন: তানভীর আহমেদ" required>
        </div>
        <div class="form-group">
          <label>আপনার মোবাইল নম্বর (যেখানে কনফার্মেশন পাবেন):</label>
          <input type="text" id="orderContact" placeholder="যেমন: 01700000000" required>
        </div>
        <div class="form-group">
          <label>পেমেন্ট মেথড:</label>
          <select id="orderMethod">
            <option value="BKASH">bKash (বিকাশ)</option>
            <option value="NAGAD">Nagad (নগদ)</option>
            <option value="ROCKET">Rocket (রকেট)</option>
          </select>
        </div>
        <div class="form-group">
          <label>যে নম্বর থেকে টাকা পাঠিয়েছেন:</label>
          <input type="text" id="orderSender" placeholder="প্রেরকের নম্বর" required>
        </div>
        <div class="form-group">
          <label>Transaction ID (TrxID):</label>
          <input type="text" id="orderTrx" placeholder="যেমন: 9X82KD71..." required>
        </div>

        <button class="btn-submit" onclick="submitWebOrder()">অর্ডার সাবমিট করুন</button>
        <div id="orderStatus" class="status-alert"></div>
      </div>
    </div>

    <!-- TAB 2: CHECK BALANCE & API KEY -->
    <div id="tab-check" class="card" style="display: none;">
      <h2 class="section-title">আপনার API Key ও ব্যালেন্স যাচাই করুন</h2>
      <p style="font-size: 13px; color: var(--subtext); margin-bottom: 14px;">এডমিন অনুমোদন দেওয়ার পর আপনাকে দেওয়া এপিআই কি দিয়ে আপনার ভার্চুয়াল নম্বর ও ব্যালেন্স দেখুন:</p>
      
      <div class="form-group">
        <label>আপনার API Key:</label>
        <input type="text" id="checkApiKey" placeholder="sec_live_xxxx...">
      </div>
      <button class="btn-submit" onclick="checkUserProfile()">যাচাই করুন</button>
      
      <div id="checkResult" class="status-alert"></div>
    </div>
  </div>

  <script>
    let selectedPackage = { name: 'US Dedicated Call & SMS', price: 350, sms: 100, min: 30 };

    function switchTab(tab) {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      if (tab === 'buy') {
        document.querySelector('.tab-btn:first-child').classList.add('active');
        document.getElementById('tab-buy').style.display = 'block';
        document.getElementById('tab-check').style.display = 'none';
      } else {
        document.querySelector('.tab-btn:last-child').classList.add('active');
        document.getElementById('tab-buy').style.display = 'none';
        document.getElementById('tab-check').style.display = 'block';
      }
    }

    function selectPkg(elem, name, price, sms, min) {
      document.querySelectorAll('.catalog-card').forEach(c => c.classList.remove('selected'));
      elem.classList.add('selected');
      selectedPackage = { name, price, sms, min };
    }

    function copyText(txt) {
      navigator.clipboard.writeText(txt);
      alert('নম্বর কপি হয়েছে: ' + txt);
    }

    async function submitWebOrder() {
      const name = document.getElementById('orderName').value.trim();
      const contact = document.getElementById('orderContact').value.trim();
      const method = document.getElementById('orderMethod').value;
      const sender = document.getElementById('orderSender').value.trim();
      const trx = document.getElementById('orderTrx').value.trim();
      const statusDiv = document.getElementById('orderStatus');

      if (!name || !contact || !trx) {
        statusDiv.className = 'status-alert error';
        statusDiv.innerText = 'অনুগ্রহ করে সকল তথ্য ও TrxID পূরণ করুন!';
        return;
      }

      statusDiv.className = 'status-alert success';
      statusDiv.innerText = 'সাবমিট হচ্ছে, অনুগ্রহ করে অপেক্ষা করুন...';

      try {
        const res = await fetch('/api/user/request-package', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userName: name,
            userContact: contact,
            smsRequested: selectedPackage.sms,
            minutesRequested: selectedPackage.min,
            totalAmountBdt: selectedPackage.price,
            paymentMethod: method,
            senderNumber: sender,
            transactionId: trx
          })
        });
        const data = await res.json();
        if (data.success) {
          statusDiv.className = 'status-alert success';
          statusDiv.innerHTML = '<strong>✓ পেমেন্ট সফলভাবে জমা হয়েছে!</strong><br>আপনার অর্ডার আইডি: ' + data.request.id + '<br>এডমিন ভেরিফাই করা মাত্র আপনার এপিআই কি ও ভার্চুয়াল নম্বর অ্যাক্টিভেট হয়ে যাবে।';
        } else {
          statusDiv.className = 'status-alert error';
          statusDiv.innerText = data.message || 'সাবমিট ব্যর্থ হয়েছে';
        }
      } catch (e) {
        statusDiv.className = 'status-alert error';
        statusDiv.innerText = 'সার্ভার সংযোগ সমস্যা। আবার চেষ্টা করুন।';
      }
    }

    async function checkUserProfile() {
      const key = document.getElementById('checkApiKey').value.trim();
      const resDiv = document.getElementById('checkResult');
      if (!key) {
        resDiv.className = 'status-alert error';
        resDiv.innerText = 'API Key লিখুন';
        return;
      }

      try {
        const res = await fetch('/api/user/profile', {
          headers: { 'x-api-key': key }
        });
        const data = await res.json();
        if (data.success && data.user) {
          resDiv.className = 'status-alert success';
          resDiv.innerHTML = '<strong>' + data.user.name + '</strong> এর একাউন্ট তথ্য:<br>' +
            '📞 বরাদ্দকৃত ভার্চুয়াল নম্বর: <strong>' + data.user.assignedNumber + '</strong><br>' +
            '✉️ SMS ব্যালেন্স: <strong>' + data.user.smsBalance + ' SMS</strong><br>' +
            '⏱️ কল মিনিট ব্যালেন্স: <strong>' + data.user.callMinutesBalance + ' মিনিট</strong><br>' +
            'স্ট্যাটাস: <strong>' + data.user.status + '</strong>';
        } else {
          resDiv.className = 'status-alert error';
          resDiv.innerText = 'এই API Key পাওয়া যায়নি বা এখনো সক্রিয় হয়নি।';
        }
      } catch (e) {
        resDiv.className = 'status-alert error';
        resDiv.innerText = 'যাচাই করতে সমস্যা হয়েছে।';
      }
    }
  </script>
</body>
</html>`;
}
