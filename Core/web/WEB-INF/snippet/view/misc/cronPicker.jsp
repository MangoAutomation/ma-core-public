<%--
    Copyright (C) 2015 Infinite Automation Software. All rights reserved.
    @author Jared Wiltshire
--%>
<%@ include file="/WEB-INF/jsp/include/tech.jsp" %>
<div class="cron-picker">
  <div class="cron-interval">
    <label for="cron-second"><fmt:message key="dateAndTime.second"/></label>
    <select name="second" id="cron-second" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="0/5"><fmt:message key="cronPicker.everyX"><fmt:param value="5"/></fmt:message></option>
      <option value="0/10"><fmt:message key="cronPicker.everyX"><fmt:param value="10"/></fmt:message></option>
      <option value="0/15"><fmt:message key="cronPicker.everyX"><fmt:param value="15"/></fmt:message></option>
      <option value="0/20"><fmt:message key="cronPicker.everyX"><fmt:param value="20"/></fmt:message></option>
      <option value="0/30"><fmt:message key="cronPicker.everyX"><fmt:param value="30"/></fmt:message></option>
      <option value="0">0</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="6">6</option>
      <option value="7">7</option>
      <option value="8">8</option>
      <option value="9">9</option>
      <option value="10">10</option>
      <option value="11">11</option>
      <option value="12">12</option>
      <option value="13">13</option>
      <option value="14">14</option>
      <option value="15">15</option>
      <option value="16">16</option>
      <option value="17">17</option>
      <option value="18">18</option>
      <option value="19">19</option>
      <option value="20">20</option>
      <option value="21">21</option>
      <option value="22">22</option>
      <option value="23">23</option>
      <option value="24">24</option>
      <option value="25">25</option>
      <option value="26">26</option>
      <option value="27">27</option>
      <option value="28">28</option>
      <option value="29">29</option>
      <option value="30">30</option>
      <option value="31">31</option>
      <option value="32">32</option>
      <option value="33">33</option>
      <option value="34">34</option>
      <option value="35">35</option>
      <option value="36">36</option>
      <option value="37">37</option>
      <option value="38">38</option>
      <option value="39">39</option>
      <option value="40">40</option>
      <option value="41">41</option>
      <option value="42">42</option>
      <option value="43">43</option>
      <option value="44">44</option>
      <option value="45">45</option>
      <option value="46">46</option>
      <option value="47">47</option>
      <option value="48">48</option>
      <option value="49">49</option>
      <option value="50">50</option>
      <option value="51">51</option>
      <option value="52">52</option>
      <option value="53">53</option>
      <option value="54">54</option>
      <option value="55">55</option>
      <option value="56">56</option>
      <option value="57">57</option>
      <option value="58">58</option>
      <option value="59">59</option>
    </select>
  </div>
  <div class="cron-interval">
    <label for="cron-minute"><fmt:message key="dateAndTime.minute"/></label>
    <select name="minute" id="cron-minute" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="0/5"><fmt:message key="cronPicker.everyX"><fmt:param value="5"/></fmt:message></option>
      <option value="0/10"><fmt:message key="cronPicker.everyX"><fmt:param value="10"/></fmt:message></option>
      <option value="0/15"><fmt:message key="cronPicker.everyX"><fmt:param value="15"/></fmt:message></option>
      <option value="0/20"><fmt:message key="cronPicker.everyX"><fmt:param value="20"/></fmt:message></option>
      <option value="0/30"><fmt:message key="cronPicker.everyX"><fmt:param value="30"/></fmt:message></option>
      <option value="0">0</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="6">6</option>
      <option value="7">7</option>
      <option value="8">8</option>
      <option value="9">9</option>
      <option value="10">10</option>
      <option value="11">11</option>
      <option value="12">12</option>
      <option value="13">13</option>
      <option value="14">14</option>
      <option value="15">15</option>
      <option value="16">16</option>
      <option value="17">17</option>
      <option value="18">18</option>
      <option value="19">19</option>
      <option value="20">20</option>
      <option value="21">21</option>
      <option value="22">22</option>
      <option value="23">23</option>
      <option value="24">24</option>
      <option value="25">25</option>
      <option value="26">26</option>
      <option value="27">27</option>
      <option value="28">28</option>
      <option value="29">29</option>
      <option value="30">30</option>
      <option value="31">31</option>
      <option value="32">32</option>
      <option value="33">33</option>
      <option value="34">34</option>
      <option value="35">35</option>
      <option value="36">36</option>
      <option value="37">37</option>
      <option value="38">38</option>
      <option value="39">39</option>
      <option value="40">40</option>
      <option value="41">41</option>
      <option value="42">42</option>
      <option value="43">43</option>
      <option value="44">44</option>
      <option value="45">45</option>
      <option value="46">46</option>
      <option value="47">47</option>
      <option value="48">48</option>
      <option value="49">49</option>
      <option value="50">50</option>
      <option value="51">51</option>
      <option value="52">52</option>
      <option value="53">53</option>
      <option value="54">54</option>
      <option value="55">55</option>
      <option value="56">56</option>
      <option value="57">57</option>
      <option value="58">58</option>
      <option value="59">59</option>
    </select>
  </div>
  <div class="cron-interval">
    <label for="cron-hour"><fmt:message key="dateAndTime.hour"/></label>
    <select name="hour" id="cron-hour" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="0/2"><fmt:message key="cronPicker.everyX"><fmt:param value="2"/></fmt:message></option>
      <option value="0/3"><fmt:message key="cronPicker.everyX"><fmt:param value="3"/></fmt:message></option>
      <option value="0/4"><fmt:message key="cronPicker.everyX"><fmt:param value="4"/></fmt:message></option>
      <option value="0/6"><fmt:message key="cronPicker.everyX"><fmt:param value="6"/></fmt:message></option>
      <option value="0/12"><fmt:message key="cronPicker.everyX"><fmt:param value="12"/></fmt:message></option>
      <option value="0">0</option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="6">6</option>
      <option value="7">7</option>
      <option value="8">8</option>
      <option value="9">9</option>
      <option value="10">10</option>
      <option value="11">11</option>
      <option value="12">12</option>
      <option value="13">13</option>
      <option value="14">14</option>
      <option value="15">15</option>
      <option value="16">16</option>
      <option value="17">17</option>
      <option value="18">18</option>
      <option value="19">19</option>
      <option value="20">20</option>
      <option value="21">21</option>
      <option value="22">22</option>
      <option value="23">23</option>
    </select>
  </div>
  <div class="cron-interval">
    <label for="cron-day-of-month"><fmt:message key="dateAndTime.dayOfMonth"/></label>
    <select name="dayOfMonth" id="cron-day-of-month" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="0/2"><fmt:message key="cronPicker.everyX"><fmt:param value="2"/></fmt:message></option>
      <option value="0/5"><fmt:message key="cronPicker.everyX"><fmt:param value="5"/></fmt:message></option>
      <option value="0/7"><fmt:message key="cronPicker.everyX"><fmt:param value="7"/></fmt:message></option>
      <option value="0/10"><fmt:message key="cronPicker.everyX"><fmt:param value="10"/></fmt:message></option>
      <option value="?"><fmt:message key="cronPicker.dontCare"/></option>
      <option value="1">1</option>
      <option value="2">2</option>
      <option value="3">3</option>
      <option value="4">4</option>
      <option value="5">5</option>
      <option value="6">6</option>
      <option value="7">7</option>
      <option value="8">8</option>
      <option value="9">9</option>
      <option value="10">10</option>
      <option value="11">11</option>
      <option value="12">12</option>
      <option value="13">13</option>
      <option value="14">14</option>
      <option value="15">15</option>
      <option value="16">16</option>
      <option value="17">17</option>
      <option value="18">18</option>
      <option value="19">19</option>
      <option value="20">20</option>
      <option value="21">21</option>
      <option value="22">22</option>
      <option value="23">23</option>
      <option value="24">24</option>
      <option value="25">25</option>
      <option value="26">26</option>
      <option value="27">27</option>
      <option value="28">28</option>
      <option value="29">29</option>
      <option value="30">30</option>
      <option value="31">31</option>
    </select>
  </div>
  <div class="cron-interval">
    <label for="cron-month"><fmt:message key="dateAndTime.month"/></label>
    <select name="month" if="cron-month" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="0/2"><fmt:message key="cronPicker.everyX"><fmt:param value="2"/></fmt:message></option>
      <option value="0/3"><fmt:message key="cronPicker.everyX"><fmt:param value="3"/></fmt:message></option>
      <option value="0/4"><fmt:message key="cronPicker.everyX"><fmt:param value="4"/></fmt:message></option>
      <option value="0/6"><fmt:message key="cronPicker.everyX"><fmt:param value="6"/></fmt:message></option>
      <option value="1"><fmt:message key="dateAndTime.january"/></option>
      <option value="2"><fmt:message key="dateAndTime.february"/></option>
      <option value="3"><fmt:message key="dateAndTime.march"/></option>
      <option value="4"><fmt:message key="dateAndTime.april"/></option>
      <option value="5"><fmt:message key="dateAndTime.may"/></option>
      <option value="6"><fmt:message key="dateAndTime.june"/></option>
      <option value="7"><fmt:message key="dateAndTime.july"/></option>
      <option value="8"><fmt:message key="dateAndTime.august"/></option>
      <option value="9"><fmt:message key="dateAndTime.september"/></option>
      <option value="10"><fmt:message key="dateAndTime.october"/></option>
      <option value="11"><fmt:message key="dateAndTime.november"/></option>
      <option value="12"><fmt:message key="dateAndTime.december"/></option>
    </select>
  </div>
  <div class="cron-interval">
    <label for="cron-day-of-week"><fmt:message key="dateAndTime.dayOfWeek"/></label>
    <select name="dayOfWeek" id="cron-day-of-week" multiple>
      <option value="*"><fmt:message key="cronPicker.every"/></option>
      <option value="1,7"><fmt:message key="dateAndTime.weekends"/></option>
      <option value="2-6"><fmt:message key="dateAndTime.weekdays"/></option>
      <option value="?"><fmt:message key="cronPicker.dontCare"/></option>
      <option value="1"><fmt:message key="dateAndTime.sunday"/></option>
      <option value="2"><fmt:message key="dateAndTime.monday"/></option>
      <option value="3"><fmt:message key="dateAndTime.tuesday"/></option>
      <option value="4"><fmt:message key="dateAndTime.wednesday"/></option>
      <option value="5"><fmt:message key="dateAndTime.thursday"/></option>
      <option value="6"><fmt:message key="dateAndTime.friday"/></option>
      <option value="7"><fmt:message key="dateAndTime.saturday"/></option>
    </select>
  </div>
</div>